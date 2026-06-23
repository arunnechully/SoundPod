package com.github.soundpod.service

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.extractor.ServiceList
import com.github.soundpod.extractor.youtube.YoutubeStreamExtractor
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@UnstableApi
class PlayerMediaSourceProvider(
    private val context: Context,
    private val cacheManager: PlayerCacheManager
) {
    private val urlCache = ConcurrentHashMap<String, Triple<Uri, String?, Long>>()
    private val resolutionLocks = ConcurrentHashMap<String, ReentrantLock>()

    internal val okHttpClient = Innertube.client.engine.let { engine ->
        if (engine is io.ktor.client.engine.okhttp.OkHttpEngine) {
            engine.config.preconfigured ?: okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        } else {
            okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        }
    }

    fun injectUrl(videoId: String, uri: Uri) {
        urlCache[videoId] = Triple(uri, null, System.currentTimeMillis())
    }
    
    companion object {
        private const val CACHE_EXPIRATION_MS = 4 * 3600000L
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
    }

    fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), DefaultExtractorsFactory())
            .setLoadErrorHandlingPolicy(YouTubeErrorPolicy(urlCache))
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(null)

        val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        val resolvingUpstreamFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
            val videoId = dataSpec.key ?: throw java.io.IOException("A key must be set")
            
            val headers = dataSpec.httpRequestHeaders.toMutableMap()
            
            if (videoId.startsWith("http") || videoId.startsWith("content://") || videoId.startsWith("file://")) {
                headers["User-Agent"] = DEFAULT_USER_AGENT
                dataSpec.buildUpon()
                    .setHttpRequestHeaders(headers)
                    .build()
            } else {
                val (uri, userAgent) = resolveUrl(videoId)
                headers["User-Agent"] = userAgent ?: DEFAULT_USER_AGENT
                
                dataSpec.withUri(uri)
                    .buildUpon()
                    .setHttpRequestHeaders(headers)
                    .build()
            }
        }

        return DataSource.Factory {
            val pauseSongCache = context.preferences.getBoolean(pauseSongCacheKey, false)

            val cacheDataSource = CacheDataSource.Factory()
                .setCache(cacheManager.cache)
                .setUpstreamDataSourceFactory(resolvingUpstreamFactory)
                .apply {
                    if (pauseSongCache) {
                        setCacheWriteDataSinkFactory(null)
                    } else {
                        setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cacheManager.cache))
                    }
                }
                .createDataSource()

            cacheDataSource
        }
    }

    fun resolveUrl(videoId: String): Pair<Uri, String?> {
        if (videoId.startsWith("http") || videoId.startsWith("content://") || videoId.startsWith("file://")) {
            return videoId.toUri() to null
        }

        urlCache[videoId]?.let { (uri, userAgent, timestamp) ->
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
                return uri to userAgent
            }
        }

        val lock = resolutionLocks.getOrPut(videoId) { ReentrantLock() }
        
        lock.withLock {
            urlCache[videoId]?.let { (uri, userAgent, timestamp) ->
                if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
                    return uri to userAgent
                }
            }
            val fastResult: Pair<Uri, String?>? = runCatching {
                val result = runBlocking { Innertube.player(videoId)?.getOrNull() }
                val uri = result?.response?.streamingData?.highestQualityFormat?.url?.toUri()
                uri?.let { it to result.userAgent }
            }.getOrNull()

            if (fastResult != null) {
                urlCache[videoId] = Triple(fastResult.first, fastResult.second, System.currentTimeMillis())
                return fastResult
            }
            val rawUrl = runCatching<String> {
                val streamExtractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
                streamExtractor.fetchPage()

                val audioStreams = streamExtractor.audioStreams

                val bestAudio: YoutubeStreamExtractor.Stream = audioStreams
                    .filter { it.codec?.lowercase(Locale.ROOT) == "opus" }
                    .maxByOrNull { it.averageBitrate }
                    ?: (audioStreams.maxByOrNull { it.averageBitrate }
                        ?: streamExtractor.videoStreams.maxByOrNull { it.bitrate }
                        ?: throw Exception("No playable streams found"))

                bestAudio.content
            }.getOrElse { e ->
                Log.e("SoundPod", "Resolution failed for $videoId", e)
                throw e
            }

            val newUri = rawUrl.toUri()
            urlCache[videoId] = Triple(newUri, null, System.currentTimeMillis())
            
            return newUri to null
        }
    }
}

@UnstableApi
private class YouTubeErrorPolicy(
    private val urlCache: ConcurrentHashMap<String, Triple<Uri, String?, Long>>
) : DefaultLoadErrorHandlingPolicy() {

    private fun isTransient(exception: Throwable): Boolean {
        var cause: Throwable? = exception
        while (cause != null) {
            if (cause is java.net.SocketException ||
                cause is java.net.SocketTimeoutException ||
                cause is java.net.UnknownHostException ||
                cause is java.net.ConnectException ||
                cause is java.io.InterruptedIOException ||
                cause is javax.net.ssl.SSLException) {
                return true
            }
            if (cause is HttpDataSource.InvalidResponseCodeException) {
                val code = cause.responseCode
                return code == 403 || code == 410 || code == 429
            }
            cause = cause.cause
        }
        return false
    }

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception
        val videoId = loadErrorInfo.loadEventInfo.dataSpec.key
        val retryCount = loadErrorInfo.errorCount

        if (isTransient(exception) && videoId != null) {
            Log.w("SoundPod", "Retrying $videoId (attempt $retryCount, transient error: $exception)")

            var cause: Throwable? = exception
            while (cause != null) {
                if (cause is HttpDataSource.InvalidResponseCodeException &&
                    (cause.responseCode == 403 || cause.responseCode == 410)) {
                    
                    urlCache.remove(videoId)
                    return if (retryCount <= 1) 0 else (1000L * retryCount).coerceAtMost(10000L)
                }
                cause = cause.cause
            }
            
            return (1000L * retryCount).coerceAtMost(10000L)
        }

        if (videoId != null) {
            Log.e("SoundPod", "Fatal error for $videoId (count $retryCount): $exception")
            urlCache.remove(videoId)
        }

        return C.TIME_UNSET
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int {
        return 3
    }
}
