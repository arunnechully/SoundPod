package com.github.soundpod.service

import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.NewPipeDownloader
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
    private val semaphore = Semaphore(5)
    private val activeTasks = ConcurrentHashMap<String, Job>()

    fun preCache(videoIds: List<String>) {
        videoIds.take(20).forEach { videoId ->
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
        if (cacheManager.isCached(videoId, 0, 512 * 1024L)) {
            Log.i("SoundPod-PreCache", "$videoId is already in cache, skipping.")
            return
        }

        Log.d("SoundPod-PreCache", "⏳ Pre-caching $videoId...")

        val result = Innertube.player(videoId)?.getOrNull()
        val response = result?.response
        
        if (response != null) {
            NewPipeDownloader.getInstance().preCache(videoId, response)
        }

        var uri = response?.streamingData?.highestQualityFormat?.url?.toUri()
        var userAgent = result?.userAgent

        if (uri == null) {
            Log.w("SoundPod-PreCache", "Innertube failed for $videoId, falling back to NewPipe")
            runCatching {
                val extractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
                extractor.fetchPage()
                val bestAudio = extractor.audioStreams
                    .filter { it.codec?.lowercase() == "opus" }
                    .maxByOrNull { it.averageBitrate }
                    ?: extractor.audioStreams.maxByOrNull { it.averageBitrate }
                
                uri = bestAudio?.content?.toUri()
                userAgent = null
            }.onFailure { e ->
                Log.e("SoundPod-PreCache", "NewPipe fallback failed for $videoId", e)
            }
        }

        if (uri != null) {
            mediaSourceProvider.injectUrl(videoId, uri)
        } else {
            Log.w("SoundPod-PreCache", "No direct URL found for $videoId after fallback")
            return
        }

        val dataSpec = DataSpec.Builder()
            .setUri(uri)
            .setKey(videoId)
            .setPosition(0)
            .setLength(256 * 1024L)
            .setHttpRequestHeaders(userAgent?.let { mapOf("User-Agent" to it) } ?: emptyMap())
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
            Log.i("SoundPod-PreCache", "Successfully buffered 256KB for $videoId")
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
