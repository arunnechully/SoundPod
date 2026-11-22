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
import com.github.innertube.requests.searchPage
import com.github.innertube.requests.song
import com.github.innertube.utils.from
import com.github.soundpod.db
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.flow.first
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
    private val sourceTracker = ConcurrentHashMap<String, Int>()

    fun getPlaybackSource(videoId: String): Int? = sourceTracker[videoId]

    fun injectUrl(videoId: String, uri: Uri) {
        urlCache[videoId] = Pair(uri, System.currentTimeMillis())
    }

    fun evictUrl(videoId: String) {
        urlCache.remove(videoId)
    }

    companion object {
        const val CACHE_EXPIRATION_MS = 4 * 3600000L
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    fun createMediaSourceFactory(): MediaSource.Factory {
        return DefaultMediaSourceFactory(createDataSourceFactory(), DefaultExtractorsFactory())
            .setLoadErrorHandlingPolicy(YouTube403ErrorPolicy(urlCache))
    }

    private fun createDataSourceFactory(): DataSource.Factory {
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(DEFAULT_USER_AGENT)

        val upstreamFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        val resolvingUpstreamFactory = ResolvingDataSource.Factory(upstreamFactory) { dataSpec ->
            val videoId = dataSpec.key ?: throw java.io.IOException("A key must be set")
            
            Log.d("SoundPod-DataSource", "Resolving URI for key: $videoId")
            val targetUri = if (videoId.startsWith("http") || videoId.startsWith("content://") || videoId.startsWith("file://") || dataSpec.uri.toString().contains("videoplayback")) {
                dataSpec.uri
            } else {
                resolveUrl(videoId)
            }

            val isYouTubeStream = targetUri.toString().contains("googlevideo.com") || targetUri.toString().contains("youtube.com")

            val headers = if (isYouTubeStream) {
                mapOf(
                    "Referer" to "https://www.youtube.com/",
                    "Origin" to "https://www.youtube.com"
                )
            } else {
                emptyMap()
            }

            dataSpec.buildUpon()
                .setUri(targetUri)
                .setHttpRequestHeaders(headers)
                .build()
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
            sourceTracker[videoId] = com.github.soundpod.R.string.cache
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

            val isPrecachedByNewPipe = runCatching {
                val precachedIds = runBlocking { db.precachedSongIds().first() }
                precachedIds.contains(videoId) || cacheManager.cache.getCachedSpans(videoId).isNotEmpty()
            }.getOrDefault(false)

            val isYouTubeTrack = videoId.length == 11 && !videoId.startsWith("http")
            var title: String? = null
            var artistsText: String? = null
            var albumTitle: String? = null
            var durationText: String? = null

            if (isPrecachedByNewPipe) {
                Log.d("SoundPod-DataSource", "Song $videoId is pre-cached by NewPipe, skipping Soundcore and continuing with NewPipe/Cache")
            } else {
                val song = runCatching { com.github.soundpod.db.getSong(videoId) }.getOrNull()
                title = song?.title
                artistsText = song?.artistsText
                durationText = song?.durationText

                if (isYouTubeTrack && title.isNullOrBlank()) {
                    val innertubeSong = runCatching {
                        runBlocking {
                            Innertube.song(videoId)?.getOrNull()
                        }
                    }.getOrNull()
                    title = innertubeSong?.info?.name
                    artistsText = innertubeSong?.authors?.mapNotNull { it.name }?.joinToString(", ")
                    albumTitle = innertubeSong?.album?.name
                    durationText = innertubeSong?.durationText
                }

                // Seamlessly try Soundcore for all tracks
                val shouldAttemptSoundcore = true

                val soundcoreUrl = if (shouldAttemptSoundcore) {
                    runCatching {
                        com.github.soundcore.Soundcore.resolveSongUrl(
                            songId = videoId,
                            title = title,
                            artistsText = artistsText,
                            albumTitle = albumTitle,
                            durationText = durationText,
                        )
                    }.getOrNull()
                } else null

                if (soundcoreUrl != null) {
                    Log.d("SoundPod-DataSource", "Soundcore resolution SUCCESS for $videoId: $soundcoreUrl")
                    val uri = soundcoreUrl.toUri()
                    urlCache[videoId] = Pair(uri, System.currentTimeMillis())
                    sourceTracker[videoId] = com.github.soundpod.R.string.soundcore
                    return uri
                }
            }

            val ytVideoId = if (!isYouTubeTrack) {
                runCatching {
                    val localSong = runCatching { com.github.soundpod.db.getSong(videoId) }.getOrNull()
                    val details = if (localSong == null) runCatching { com.github.soundcore.Soundcore.getSongDetails(videoId) }.getOrNull() else null
                    val t = localSong?.title ?: details?.title ?: title
                    val a = localSong?.artistsText ?: details?.artistsText ?: artistsText
                    val query = listOfNotNull(t, a).filter { it.isNotBlank() }.joinToString(" ").trim()
                    if (query.isNotBlank()) {
                        runBlocking {
                            Innertube.searchPage(
                                query = query,
                                params = Innertube.SearchFilter.Song.value,
                                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                            )?.getOrNull()?.items?.firstOrNull()?.info?.endpoint?.videoId
                        }
                    } else null
                }.getOrNull() ?: videoId
            } else {
                videoId
            }

            val rawUrl = runCatching {
                    val streamExtractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$ytVideoId")
                    streamExtractor.fetchPage()

                    val audioStreams = streamExtractor.audioStreams

                    val bestAudio = audioStreams
                        .filter { it.codec?.lowercase(Locale.ROOT) == "opus" }
                        .maxByOrNull { it.averageBitrate }
                        ?: audioStreams.maxByOrNull { it.averageBitrate }
                        ?: streamExtractor.videoStreams.maxByOrNull { it.bitrate }
                        ?: throw Exception("No playable streams found by NewPipe for $ytVideoId")

                    bestAudio.content
                }.getOrElse { e ->
                    Log.e("SoundPod-Debug", "All resolution methods failed for $videoId (yt: $ytVideoId)", e)
                    return "https://www.youtube.com/watch?v=$ytVideoId".toUri()
                }

                val newUri = rawUrl.toUri()
                urlCache[videoId] = Pair(newUri, System.currentTimeMillis())
                sourceTracker[videoId] = com.github.soundpod.R.string.newpipe

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
