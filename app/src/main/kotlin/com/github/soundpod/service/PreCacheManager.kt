package com.github.soundpod.service

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.db
import com.github.soundpod.models.PrecachedSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    private val semaphore = Semaphore(3)
    private val activeTasks = ConcurrentHashMap<String, Job>()

    fun preCache(videoIds: List<String>) {
        if (videoIds.isEmpty()) return
        Log.d("SoundPod-PreCache", "Requested pre-cache for ${videoIds.size} songs: ${videoIds.joinToString(", ")}")
        videoIds.take(20).forEach { videoId ->
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
        Log.d("SoundPod-PreCache", "⏳ Pre-caching $videoId (using InnerTube)...")
        
        var bitrate = 128_000L // Default fallback
        var playerResponseCache: com.github.innertube.models.PlayerResponse? = null
        val uri: Uri? = try {
            val playerResponse = runBlocking {
                Innertube.player(videoId)?.getOrNull()
            }
            playerResponseCache = playerResponse
            val streamExtractor = com.github.soundpod.extractor.youtube.YoutubeStreamExtractor(
                "https://www.youtube.com/watch?v=$videoId",
                playerResponse
            )
            streamExtractor.fetchPage()
            
            val lowQualityAudio = streamExtractor.lowestQualityAudioStream
            
            if (lowQualityAudio == null) {
                Log.w("SoundPod-PreCache", "No streams found at all for $videoId")
            } else {
                bitrate = lowQualityAudio.averageBitrate
                Log.d("SoundPod-PreCache", "Using low-quality stream for $videoId (bitrate: $bitrate)")
            }

            lowQualityAudio?.content?.takeIf { it.isNotBlank() }?.toUri()
        } catch (e: Exception) {
            Log.e("SoundPod-PreCache", "InnerTube extraction failed for $videoId", e)
            null
        }

        if (uri != null) {
            val playbackSource = if (playerResponseCache != null) {
                "InnerTube (${playerResponseCache.clientName ?: "Unknown Client"}) (Pre-cache)"
            } else "InnerTube (Pre-cache)"
            mediaSourceProvider.injectUrl(videoId, uri, isLowQuality = true, playbackSource = playbackSource)
        } else {
            Log.w("SoundPod-PreCache", "No direct URL found for $videoId via InnerTube")
            return
        }

        val dynamicLength = ((bitrate / 8) * 1.5).toLong().coerceIn(32 * 1024L, 256 * 1024L)

        if (cacheManager.isCached(videoId, 0, dynamicLength)) {
            Log.i("SoundPod-PreCache", "$videoId is already in cache (at least ${dynamicLength / 1024}KB), skipping fetch.")
            db.insert(PrecachedSong(videoId))
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
