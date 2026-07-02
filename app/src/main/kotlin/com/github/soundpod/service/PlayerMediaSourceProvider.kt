package com.github.soundpod.service

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.extractor.youtube.YoutubeStreamExtractor
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@UnstableApi
class PlayerMediaSourceProvider(
    private val context: Context,
    private val cacheManager: PlayerCacheManager
) {
    // Triple: uri, userAgent, timestamp
    // We add a Fourth element to mark if it's low quality: Triple(uri, userAgent, timestamp) -> Pair(Triple, isLowQuality)
    private val urlCache = ConcurrentHashMap<String, CacheEntry>()
    private val resolutionLocks = ConcurrentHashMap<String, ReentrantLock>()

    internal val okHttpClient = Innertube.okHttpClient

    data class CacheEntry(
        val uri: Uri,
        val userAgent: String?,
        val timestamp: Long,
        val isLowQuality: Boolean = false,
        val playbackSource: String? = null
    )

    fun injectUrl(videoId: String, uri: Uri, isLowQuality: Boolean = false, playbackSource: String? = "NewPipe Extractor") {
        urlCache[videoId] = CacheEntry(uri, null, System.currentTimeMillis(), isLowQuality, playbackSource)
    }
    
    companion object {
        private const val CACHE_EXPIRATION_MS = 4 * 3600000L
    }

    fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), DefaultExtractorsFactory())
            .setLoadErrorHandlingPolicy(YouTubeErrorPolicy(urlCache))
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(Innertube.USER_AGENT)

        val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        val resolvingUpstreamFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
            val videoId = dataSpec.key ?: throw java.io.IOException("A key must be set")
            
            val headers = dataSpec.httpRequestHeaders.toMutableMap()
            
            if (videoId.startsWith("http") || videoId.startsWith("content://") || videoId.startsWith("file://")) {
                headers["User-Agent"] = Innertube.USER_AGENT
                dataSpec.buildUpon()
                    .setHttpRequestHeaders(headers)
                    .build()
            } else {
                val (uri, userAgent) = resolveUrl(videoId)
                headers["User-Agent"] = userAgent ?: Innertube.USER_AGENT
                
                dataSpec.withUri(uri)
                    .buildUpon()
                    .setHttpRequestHeaders(headers)
                    .build()
            }
        }

        return DataSource.Factory {
            val pauseSongCache = context.preferences.getBoolean(pauseSongCacheKey, false)

            val transientCacheDataSource = CacheDataSource.Factory()
                .setCache(cacheManager.cache)
                .setUpstreamDataSourceFactory(resolvingUpstreamFactory)
                .setCacheWriteDataSinkFactory { // This fallback is needed if the dataSpec isn't available yet or for default creation
                    CacheDataSink.Factory().setCache(cacheManager.cache).createDataSink()
                }

                // Note: In Media3 1.1+, setCacheWriteDataSinkFactory can sometimes be bypassed or
                // replaced by logic that checks for specific videoId.
                // Since CacheDataSource.Factory doesn't easily provide DataSpec to the Factory's createDataSink(),
                // we'll use this approach and assume the caller might be using a more complex setup.
                // However, to keep it simple and buffer-safe, we'll implement a custom DataSource if needed.
                // For now, we'll try to use the pauseSongCache flag globally within the factory.

                .apply {
                    if (pauseSongCache) {
                        setCacheWriteDataSinkFactory(null)
                    }
                }
                .createDataSource()

            val downloadCacheDataSource = CacheDataSource.Factory()
                .setCache(cacheManager.downloadCache)
                .setUpstreamDataSourceFactory { transientCacheDataSource }
                .setCacheWriteDataSinkFactory(null)
                .createDataSource()

            downloadCacheDataSource
        }
    }

    fun resolveUrl(videoId: String): Pair<Uri, String?> {
        if (videoId.startsWith("http") || videoId.startsWith("content://") || videoId.startsWith("file://")) {
            return videoId.toUri() to null
        }

        urlCache[videoId]?.let { entry ->
            if (!entry.isLowQuality && System.currentTimeMillis() - entry.timestamp < CACHE_EXPIRATION_MS) {
                return entry.uri to entry.userAgent
            }
        }

        val lock = resolutionLocks.getOrPut(videoId) { ReentrantLock() }
        lock.withLock {
            urlCache[videoId]?.let { entry ->
                if (!entry.isLowQuality && System.currentTimeMillis() - entry.timestamp < CACHE_EXPIRATION_MS) {
                    return entry.uri to entry.userAgent
                }
            }

            Log.d("SoundPod", "Resolving URL for $videoId")

            val playerResponse = runBlocking { Innertube.player(videoId)?.getOrNull() }

            val streamExtractor = YoutubeStreamExtractor("https://www.youtube.com/watch?v=$videoId", playerResponse)
            streamExtractor.fetchPage()

            val audioStreams = streamExtractor.audioStreams
            if (audioStreams.isEmpty()) throw Exception("No audio streams found for $videoId")

            val bestAudio: YoutubeStreamExtractor.Stream = audioStreams
                .filter { it.content.isNotBlank() }
                .maxByOrNull { it.averageBitrate }
                ?: throw Exception("No playable audio streams found for $videoId")

            if (bestAudio.content.isBlank()) throw Exception("Empty stream content for $videoId")
            Log.d("SoundPod", "Selected stream for $videoId: ${bestAudio.codec} (${bestAudio.averageBitrate / 1000}kbps)")
            val uri = bestAudio.content.toUri()

            urlCache[videoId] = CacheEntry(uri, null, System.currentTimeMillis(), playbackSource = "SoundPod Resolver")
            return uri to null
        }
    }

    fun getPlaybackSource(videoId: String): String? = urlCache[videoId]?.playbackSource
}

@UnstableApi
private class YouTubeErrorPolicy(
    private val urlCache: ConcurrentHashMap<String, PlayerMediaSourceProvider.CacheEntry>
) : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val throwable = loadErrorInfo.exception
        if (throwable is HttpDataSource.InvalidResponseCodeException &&
            (throwable.responseCode == 403 || throwable.responseCode == 404)
        ) {
            val videoId = loadErrorInfo.loadEventInfo.dataSpec.key
            if (videoId != null) {
                Log.w("SoundPod", "Forbidden or Not Found for $videoId, clearing cache and retrying")
                urlCache.remove(videoId)
            }
            return 0
        }

        return super.getRetryDelayMsFor(loadErrorInfo)
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int {
        return 3
    }
}
