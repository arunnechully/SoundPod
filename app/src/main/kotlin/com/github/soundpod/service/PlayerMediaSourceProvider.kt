package com.github.soundpod.service

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.db
import com.github.soundpod.query
import com.github.soundpod.utils.RingBuffer
import com.github.soundpod.utils.findNextMediaItemById
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@UnstableApi
class PlayerMediaSourceProvider(
    private val context: Context,
    private val cacheManager: PlayerCacheManager,
    private val playerProvider: () -> ExoPlayer
) {

    fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), DefaultExtractorsFactory())
    }

    private fun createCacheDataSource(): DataSource.Factory {
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(16000)
            .setReadTimeoutMs(8000)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0")
        
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
        val chunkLength = 512 * 1024L
        val ringBuffer = RingBuffer<Pair<String, Uri>?>(2) { null }

        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val videoId = dataSpec.key ?: throw java.io.IOException("A key must be set")

            if (cacheManager.cache.isCached(videoId, dataSpec.position, chunkLength)) {
                dataSpec
            } else {
                when (videoId) {
                    ringBuffer.getOrNull(0)?.first -> dataSpec.withUri(ringBuffer.getOrNull(0)!!.second)
                    ringBuffer.getOrNull(1)?.first -> dataSpec.withUri(ringBuffer.getOrNull(1)!!.second)
                    else -> {
                        val urlResult = runBlocking(Dispatchers.IO) {
                            Innertube.player(videoId = videoId)
                        }?.mapCatching { body ->

                            when (val status = body.playabilityStatus?.status) {
                                "OK" -> {
                                    val stream = body.streamingData?.adaptiveFormats
                                        ?.filter { it.mimeType.contains("audio") && it.url != null }
                                        ?.maxByOrNull {
                                            when (it.itag) {
                                                251 -> 1000000L
                                                140 -> 900000L
                                                else -> it.bitrate ?: 0L
                                            }
                                        }
                                        ?: body.streamingData?.formats?.firstOrNull { it.url != null } // Fallback to progressive

                                    val player = playerProvider()
                                    val mediaItem = runBlocking(Dispatchers.Main) {
                                        player.findNextMediaItemById(videoId)
                                    }
                                    if (mediaItem?.mediaMetadata?.extras?.getString("durationText") == null) {
                                    }

                                    query {
                                        mediaItem?.let(db::insert)
                                        db.insert(
                                            com.github.soundpod.models.Format(
                                                songId = videoId,
                                                itag = stream?.itag,
                                                mimeType = stream?.mimeType,
                                                bitrate = stream?.bitrate,
                                                loudnessDb = body.playerConfig?.audioConfig?.normalizedLoudnessDb,
                                                contentLength = stream?.contentLength,
                                                lastModified = stream?.lastModified
                                            )
                                        )
                                    }

                                    stream?.url ?: throw Exception("PlayableFormatNotFoundException: Stream URL is null (Possible encrypted stream)")
                                }

                                "UNPLAYABLE" -> {
                                    val reason = body.playabilityStatus?.reason ?: "No reason provided"
                                    Log.e("SoundPod-Debug", "UNPLAYABLE RESPONSE ($videoId): $reason")
                                    throw java.io.IOException("Unplayable: $reason")
                                }
                                "LOGIN_REQUIRED" -> throw java.io.IOException("LoginRequiredException")
                                else -> throw java.io.IOException("Remote error ($status): ${body.playabilityStatus?.reason}")
                            }
                        }

                        urlResult?.getOrThrow()?.let { url ->
                            ringBuffer.append(videoId to url.toUri())
                            dataSpec.withUri(url.toUri())
                                .subrange(dataSpec.uriPositionOffset, chunkLength)
                        } ?: throw java.io.IOException(
                            "Failed to resolve URL",
                            urlResult?.exceptionOrNull()
                        )
                    }
                }
            }
        }
    }
}