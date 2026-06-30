package com.github.soundpod.service

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.github.soundpod.db
import com.github.soundpod.extractor.ServiceList
import com.github.soundpod.models.PrecachedSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@UnstableApi
class PreCacheManager(
    private val cacheManager: PlayerCacheManager,
    private val mediaSourceProvider: PlayerMediaSourceProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val semaphore = Semaphore(10)
    private val activeTasks = ConcurrentHashMap<String, Job>()

    fun preCache(videoIds: List<String>) {
        if (videoIds.isEmpty()) return
        Log.d("SoundPod-PreCache", "Requested pre-cache for ${videoIds.size} songs: ${videoIds.joinToString(", ")}")
        videoIds.take(50).forEach { videoId ->
            if (videoId.isBlank()) return@forEach
            
            // Avoid duplicate active tasks
            if (activeTasks.containsKey(videoId)) {
                Log.d("SoundPod-PreCache", "Task for $videoId already active, skipping request")
                return@forEach
            }
            
            val job = scope.launch {
                try {
                    semaphore.withPermit {
                        Log.d("SoundPod-PreCache", "Starting pre-cache task for $videoId")
                        preCacheSong(videoId)
                    }
                } catch (e: Exception) {
                    Log.e("SoundPod-PreCache", "Unexpected error in pre-cache job for $videoId", e)
                } finally {
                    activeTasks.remove(videoId)
                }
            }
            activeTasks[videoId] = job
        }
    }

    private fun preCacheSong(videoId: String) {
        Log.d("SoundPod-PreCache", "⏳ Pre-caching $videoId (using NewPipe)...")
        com.github.soundpod.extractor.NewPipeHelper.init()

        var bitrate = 128_000L // Default fallback
        val uri: Uri? = try {
            val streamExtractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId", null)
            streamExtractor.fetchPage()
            
            val bestAudio = streamExtractor.lowestQualityAudioStream
            
            if (bestAudio == null) {
                Log.w("SoundPod-PreCache", "No streams found at all for $videoId")
            } else {
                bitrate = bestAudio.averageBitrate
                Log.d("SoundPod-PreCache", "Using low-quality stream for $videoId (bitrate: $bitrate)")
            }

            bestAudio?.content?.takeIf { it.isNotBlank() }?.toUri()
        } catch (e: Exception) {
            Log.e("SoundPod-PreCache", "NewPipe extraction failed for $videoId", e)
            null
        }

        if (uri != null) {
            mediaSourceProvider.injectUrl(videoId, uri, isLowQuality = true)
        } else {
            Log.w("SoundPod-PreCache", "No direct URL found for $videoId via NewPipe")
            return
        }

        // Calculate bytes for ~1.5 seconds of playback
        // Bitrate is in bits per second, so divide by 8 for bytes
        val dynamicLength = ((bitrate / 8) * 1.5).toLong().coerceIn(32 * 1024L, 256 * 1024L)

        if (cacheManager.isCached(videoId, 0, dynamicLength)) {
            Log.i("SoundPod-PreCache", "$videoId is already in cache (at least ${dynamicLength / 1024}KB), skipping.")
            return
        }

        val dataSpec = DataSpec.Builder()
            .setUri(uri)
            .setKey(videoId)
            .setPosition(0)
            .setLength(dynamicLength)
            .build()

        val upstreamDataSource = OkHttpDataSource.Factory(mediaSourceProvider.okHttpClient)
            .createDataSource()

        val cacheDataSource = CacheDataSource.Factory()
            .setCache(cacheManager.cache)
            .setUpstreamDataSourceFactory { upstreamDataSource }
            .createDataSource()

        try {
            CacheWriter(cacheDataSource, dataSpec, null, null).cache()
            db.insert(PrecachedSong(videoId))
            Log.i("SoundPod-PreCache", "Successfully buffered ${dynamicLength / 1024}KB for $videoId (Bitrate: ${bitrate / 1000}kbps)")
        } catch (e: Exception) {
            Log.e("SoundPod-PreCache", "Caching failed for $videoId: ${e.message}")
        }
    }

    fun cleanUp() {
        scope.launch {
            val threshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
            val oldSongs = db.oldPrecachedSongs(threshold)
            
            oldSongs.forEach { song ->
                cacheManager.removeCache(song.id)
                db.deletePrecachedSong(song.id)
                Log.d("SoundPod-PreCache", "Cleaned up expired pre-cache for ${song.id}")
            }
        }
    }
}
