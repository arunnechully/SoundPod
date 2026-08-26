package com.github.soundpod.service

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.utils.PlaybackSource
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.playbackSourceKey
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.ServiceList
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@UnstableApi
class PlayerMediaSourceProvider(
    private val context: Context,
    private val cacheManager: PlayerCacheManager
) {
    private val urlCache = ConcurrentHashMap<String, Pair<Uri, Long>>()
    private val resolutionLocks = ConcurrentHashMap<String, ReentrantLock>()

    fun injectUrl(videoId: String, uri: Uri) {
        urlCache[videoId] = Pair(uri, System.currentTimeMillis())
    }

    fun evictUrl(videoId: String) {
        urlCache.remove(videoId)
    }

    companion object {
        const val CACHE_EXPIRATION_MS = 4 * 3600000L
        const val DEFAULT_USER_AGENT = "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 14; eureka-user Build/SQ3A.220605.009.A1) gzip"
    }

    fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), DefaultExtractorsFactory())
            .setLoadErrorHandlingPolicy(YouTube403ErrorPolicy(urlCache))
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val playbackSource = PlaybackSource.NewPipe // Forced to NewPipe temporarily
        
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val httpDataSourceFactory = if (playbackSource == PlaybackSource.NewPipe) {
            OkHttpDataSource.Factory(okHttpClient)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        } else {
            OkHttpDataSource.Factory(okHttpClient)
                .setUserAgent(DEFAULT_USER_AGENT)
                .setDefaultRequestProperties(buildMap {
                    put("Referer", "https://www.youtube.com/")
                    put("Origin", "https://www.youtube.com")
                    put("X-YouTube-Client-Name", "28")
                    put("X-YouTube-Client-Version", "1.71.26")
                    Innertube.visitorData?.let { put("X-Goog-Visitor-Id", it) }
                    Innertube.cookies?.let { put("Cookie", it) }
                })
        }

        val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        val resolvingUpstreamFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
            val videoId = dataSpec.key ?: throw java.io.IOException("A key must be set")
            Log.d("SoundPod-DataSource", "Resolving URI for key: $videoId")
            if (videoId.startsWith("http") || videoId.startsWith("content://") || videoId.startsWith("file://")) {
                dataSpec
            } else {
                val uri = resolveUrl(videoId)
                dataSpec.withUri(uri)
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

    fun resolveUrl(videoId: String): Uri {
        if (videoId.startsWith("http") || videoId.startsWith("content://") || videoId.startsWith("file://")) {
            return videoId.toUri()
        }

        // Check if fully cached first to avoid network calls in offline mode
        val metadata = cacheManager.cache.getContentMetadata(videoId)
        val length = ContentMetadata.getContentLength(metadata)
        if (length > 0 && cacheManager.cache.isCached(videoId, 0, length)) {
            Log.d("SoundPod-DataSource", "Cache HIT for $videoId ($length bytes), bypassing resolution")
            return urlCache[videoId]?.first ?: "https://www.youtube.com/watch?v=$videoId".toUri()
        }

        urlCache[videoId]?.let { (uri, timestamp) ->
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
                Log.d("SoundPod-DataSource", "URL cache hit for $videoId")
                return uri
            }
        }

        val lock = resolutionLocks.getOrPut(videoId) { ReentrantLock() }

        lock.withLock {
            urlCache[videoId]?.let { (uri, timestamp) ->
                if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
                    return uri
                }
            }

            val playbackSource = PlaybackSource.NewPipe // Forced to NewPipe temporarily

            if (playbackSource == PlaybackSource.Automatic || playbackSource == PlaybackSource.Innertube) {
                // TRY INNERTUBE FIRST (MUCH FASTER)
                val fastUri: Uri? = runCatching {
                    val response = runBlocking { Innertube.player(videoId)?.getOrNull() }
                    response?.streamingData?.highestQualityFormat?.url?.toUri()
                }.getOrNull()

                if (fastUri != null) {
                    urlCache[videoId] = Pair(fastUri, System.currentTimeMillis())
                    return fastUri
                }
            }

            if (playbackSource == PlaybackSource.Innertube) {
                throw Exception("Innertube resolution failed for $videoId")
            }

            // FALLBACK TO NEWPIPE (SLOWER)
            val rawUrl = runCatching {
                val streamExtractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
                streamExtractor.fetchPage()

                val audioStreams = streamExtractor.audioStreams

                val bestAudio = audioStreams
                    .filter { it.codec?.lowercase(Locale.ROOT) == "opus" }
                    .maxByOrNull { it.averageBitrate }
                    ?: audioStreams.maxByOrNull { it.averageBitrate }
                    ?: streamExtractor.videoStreams.maxByOrNull { it.bitrate }
                    ?: throw Exception("No playable streams found by NewPipe for $videoId")

                bestAudio.content
            }.getOrElse { e ->
                Log.e("SoundPod-Debug", "NewPipe resolution failed for $videoId (Network likely down)", e)
                // If we're offline, return a generic URI. CacheDataSource will still check the cache key.
                return "https://www.youtube.com/watch?v=$videoId".toUri()
            }

            val newUri = rawUrl.toUri()
            urlCache[videoId] = Pair(newUri, System.currentTimeMillis())

            return newUri
        }
    }
}

@UnstableApi
private class YouTube403ErrorPolicy(
    private val urlCache: ConcurrentHashMap<String, Pair<Uri, Long>>
) : DefaultLoadErrorHandlingPolicy() {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception

        if (exception is HttpDataSource.InvalidResponseCodeException && exception.responseCode == 403) {
            val videoId = loadErrorInfo.loadEventInfo.dataSpec.key
            Log.w("SoundPod-Debug", "Hit a 403 Forbidden for $videoId! Evicting URL cache and retrying...")

            if (videoId != null) {
                urlCache.remove(videoId)
            } else {
                urlCache.clear()
            }
            return 1000L
        }

        return super.getRetryDelayMsFor(loadErrorInfo)
    }
}
