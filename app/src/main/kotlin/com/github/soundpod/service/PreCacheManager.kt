package com.github.soundpod.service

import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.NewPipeDownloader
import com.github.soundpod.db
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
    private val semaphore = Semaphore(1)
    private val activeTasks = ConcurrentHashMap<String, Job>()

    fun preCache(videoIds: List<String>) {
        videoIds.take(5).forEach { videoId ->
            if (videoId.isBlank()) return@forEach

            // Avoid duplicate active tasks
            if (activeTasks.containsKey(videoId)) return@forEach

            val job = scope.launch {
                try {
                    semaphore.withPermit {
                        preCacheSong(videoId)
                    }
                } finally {
                    activeTasks.remove(videoId)
                }
            }
            activeTasks[videoId] = job
        }
    }

    private suspend fun preCacheSong(videoId: String) {
        if (cacheManager.isCached(videoId, 0, 128 * 1024L)) {
            Log.i("SoundPod-PreCache", "$videoId is already in cache, skipping.")
            return
        }

        Log.d("SoundPod-PreCache", "Pre-caching $videoId...")

        val response = Innertube.player(videoId)?.getOrNull()
        if (response == null) {
            Log.e("SoundPod-PreCache", "Failed to get metadata for $videoId")
            return
        }

        NewPipeDownloader.getInstance().preCache(videoId, response)
        val bestFormat = response.streamingData?.highestQualityFormat

        val initialUri = bestFormat?.url?.toUri()

        val finalUri: android.net.Uri = if (initialUri != null) {
            mediaSourceProvider.injectUrl(videoId, initialUri)
            initialUri
        } else {
            Log.w("SoundPod-PreCache", "No direct URL found for $videoId, waiting for resolver...")
            val resolvedUri = runCatching { mediaSourceProvider.resolveUrl(videoId) }.getOrNull()

            if (resolvedUri != null) {
                resolvedUri
            } else {
                Log.e("SoundPod-PreCache", "Totally failed to resolve direct URL for $videoId")
                return
            }
        }
        val dataSpec = DataSpec.Builder()
            .setUri(finalUri)
            .setKey(videoId)
            .setPosition(0)
            .setLength(128 * 1024L)
            .build()

        val upstreamDataSource = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .createDataSource()

        val cacheDataSource = CacheDataSource.Factory()
            .setCache(cacheManager.cache)
            .setUpstreamDataSourceFactory { upstreamDataSource }
            .createDataSource()

        try {
            CacheWriter(cacheDataSource, dataSpec, null, null).cache()
            db.insert(PrecachedSong(videoId))
            Log.i("SoundPod-PreCache", "Successfully buffered 128kb for $videoId")
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
