package com.github.soundpod.service

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.preferences
import org.schabi.newpipe.extractor.ServiceList
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
    
    companion object {
        private const val CACHE_EXPIRATION_MS = 4 * 3600000L
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), DefaultExtractorsFactory())
            .setLoadErrorHandlingPolicy(YouTube403ErrorPolicy(urlCache))
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(16000)
            .setReadTimeoutMs(8000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(DEFAULT_USER_AGENT)

        val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        val resolvingUpstreamFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
            val videoId = dataSpec.key ?: throw java.io.IOException("A key must be set")
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

        urlCache[videoId]?.let { (uri, timestamp) ->
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MS) {
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

            val rawUrl = runCatching {
                val streamExtractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
                streamExtractor.fetchPage()

                val audioStreams = streamExtractor.audioStreams

                val bestAudio = audioStreams
                    .filter { it.codec?.lowercase() == "opus" }
                    .maxByOrNull { it.averageBitrate }
                    ?: audioStreams.maxByOrNull { it.averageBitrate }
                    ?: streamExtractor.videoStreams.maxByOrNull { it.bitrate }
                    ?: throw Exception("No playable streams found by NewPipe for $videoId")

                bestAudio.content
            }.getOrElse { e ->
                Log.e("SoundPod-Debug", "NewPipe resolution failed for $videoId", e)
                throw e
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
