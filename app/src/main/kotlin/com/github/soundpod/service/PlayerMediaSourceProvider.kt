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
import com.github.soundpod.db
import com.github.soundpod.query
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.preferences
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.VideoStream
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
        private const val CACHE_EXPIRATION_MS = 3600000L
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.193 Mobile Safari/537.36"
    }

    fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), DefaultExtractorsFactory())
            .setLoadErrorHandlingPolicy(YouTube403ErrorPolicy(urlCache))
    }

    private fun createCacheDataSource(): DataSource.Factory {
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(16000)
            .setReadTimeoutMs(8000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(DEFAULT_USER_AGENT)

        return DataSource.Factory {
            val pauseSongCache = context.preferences.getBoolean(pauseSongCacheKey, false)

            val cacheFactory = CacheDataSource.Factory()
                .setCache(cacheManager.cache)
                .setUpstreamDataSourceFactory(upstreamFactory)

            if (pauseSongCache) {
                cacheFactory.setCacheWriteDataSinkFactory(null)
            } else {
                cacheFactory.setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cacheManager.cache))
            }
            cacheFactory.createDataSource()
        }
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val videoId = dataSpec.key ?: throw java.io.IOException("A key must be set")
            if (cacheManager.cache.isCached(videoId, dataSpec.position, 100 * 1024L)) {
                dataSpec
            } else {
                val uri = resolveUrl(videoId)
                dataSpec.withUri(uri)
            }
        }
    }

    fun resolveUrl(videoId: String): Uri {
        val lock = resolutionLocks.getOrPut(videoId) { ReentrantLock() }
        
        lock.withLock {
            val cachedEntry = urlCache[videoId]
            val currentTime = System.currentTimeMillis()

            if (cachedEntry != null && (currentTime - cachedEntry.second) < CACHE_EXPIRATION_MS) {
                return cachedEntry.first
            }

            val urlResult = runCatching {
                val streamExtractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
                streamExtractor.fetchPage()

                val audioStreams = streamExtractor.audioStreams
                val videoStreams = streamExtractor.videoStreams

                val bestAudio = audioStreams.maxByOrNull { it.averageBitrate }
                    ?: videoStreams.maxByOrNull { it.bitrate }
                    ?: audioStreams.firstOrNull()
                    ?: videoStreams.firstOrNull()
                    ?: throw Exception("No playable streams found by NewPipe for videoId: $videoId")

                val song = com.github.soundpod.models.Song(
                    id = videoId,
                    title = streamExtractor.name,
                    artistsText = streamExtractor.uploaderName,
                    durationText = null,
                    thumbnailUrl = streamExtractor.thumbnails.firstOrNull()?.url
                )

                query {
                    db.insert(song)
                    db.insert(
                        com.github.soundpod.models.Format(
                            songId = videoId,
                            itag = when (bestAudio) {
                                is AudioStream -> bestAudio.formatId
                                is VideoStream -> bestAudio.formatId
                                else -> -1
                            },
                            mimeType = bestAudio.format?.mimeType,
                            bitrate = when (bestAudio) {
                                is AudioStream -> bestAudio.averageBitrate.toLong()
                                is VideoStream -> bestAudio.bitrate.toLong()
                                else -> -1L
                            },
                            loudnessDb = null,
                            contentLength = null,
                            lastModified = null
                        )
                    )
                }

                bestAudio.content
            }

            val rawUrl = urlResult.getOrThrow()
            val newUri = rawUrl.toUri()
            urlCache[videoId] = Pair(newUri, System.currentTimeMillis())
            
            // Clean up the lock map to prevent leaks
            resolutionLocks.remove(videoId)
            
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