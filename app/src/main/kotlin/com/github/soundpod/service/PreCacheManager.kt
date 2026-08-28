package com.github.soundpod.service

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.MainApplication
import com.github.soundpod.NewPipeDownloader
import com.github.soundpod.db
import com.github.soundpod.models.DownloadedSong
import com.github.soundpod.models.Format
import com.github.soundpod.models.PrecachedSong
import com.github.soundpod.models.Song
import com.github.soundpod.utils.PlaybackSource
import com.github.soundpod.utils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import java.util.Locale
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

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun preCache(mediaItems: List<MediaItem>) {
        mediaItems.take(5).forEach { mediaItem ->
            val videoId = mediaItem.mediaId
            if (videoId.isBlank()) return@forEach

            // Avoid duplicate active tasks
            if (activeTasks.containsKey(videoId)) return@forEach

            val job = scope.launch {
                try {
                    semaphore.withPermit {
                        preCacheSong(videoId, 128 * 1024L, mediaItem)
                    }
                } finally {
                    activeTasks.remove(videoId)
                }
            }
            activeTasks[videoId] = job
        }
    }

    fun cacheFull(mediaItem: MediaItem) {
        val videoId = mediaItem.mediaId
        if (videoId.isBlank()) return
        
        // Cancel existing pre-cache task if any to prioritize full download
        activeTasks[videoId]?.cancel()
        
        val job = scope.launch {
            // Mark as downloaded immediately so it shows up in the Offline playlist
            // Ensure song exists in DB first to satisfy FK constraints
            ensureSongExists(videoId, mediaItem)
            db.insert(DownloadedSong(videoId))

            try {
                semaphore.withPermit {
                    preCacheSong(videoId, -1L, mediaItem)
                }
            } finally {
                activeTasks.remove(videoId)
            }
        }
        activeTasks[videoId] = job
    }

    private suspend fun ensureSongExists(videoId: String, mediaItem: MediaItem?) {
        mediaItem?.let { item ->
            val songExists = db.song(videoId).first() != null
            if (!songExists) {
                db.insert(
                    Song(
                        id = videoId,
                        title = item.mediaMetadata.title?.toString() ?: videoId,
                        artistsText = item.mediaMetadata.artist?.toString(),
                        durationText = item.mediaMetadata.extras?.getString("durationText"),
                        thumbnailUrl = item.mediaMetadata.artworkUri?.toString()
                    )
                )
            }
        }
    }

    private suspend fun preCacheSong(videoId: String, length: Long, mediaItem: MediaItem? = null) {
        Log.d("SoundPod-PreCache", "Pre-caching $videoId (requested length: $length)...")

        // Ensure song exists in DB to avoid FK constraint crashes
        ensureSongExists(videoId, mediaItem)

        val playbackSource = PlaybackSource.NewPipe // Forced to NewPipe temporarily

        var finalUri: android.net.Uri? = null
        var contentLength: Long? = null
        var bitrate: Long? = null
        var itag: Int? = null
        var mimeType: String? = null

        if (playbackSource == PlaybackSource.Automatic || playbackSource == PlaybackSource.Innertube) {
            val response = Innertube.player(videoId)?.getOrNull()
            if (response != null) {
                NewPipeDownloader.getInstance().preCache(videoId, response)
                val bestFormat = response.streamingData?.highestQualityFormat
                finalUri = bestFormat?.url?.toUri()
                contentLength = bestFormat?.contentLength
                bitrate = bestFormat?.bitrate
                itag = bestFormat?.itag
                mimeType = bestFormat?.mimeType
            }
        }

        if (finalUri == null) {
            if (playbackSource == PlaybackSource.Innertube) {
                Log.e("SoundPod-PreCache", "Innertube resolution failed for $videoId")
                return
            }
            
            // USE NEWPIPE EXTRACTOR
            runCatching {
                val streamExtractor = ServiceList.YouTube.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
                streamExtractor.fetchPage()

                val audioStreams = streamExtractor.audioStreams
                val bestAudio = audioStreams
                    .filter { it.codec?.lowercase(Locale.ROOT) == "opus" }
                    .maxByOrNull { it.averageBitrate }
                    ?: audioStreams.maxByOrNull { it.averageBitrate }

                if (bestAudio != null) {
                    finalUri = bestAudio.content.toUri()
                    bitrate = bestAudio.averageBitrate.toLong()
                    itag = bestAudio.itag
                    mimeType = "audio/webm"
                    contentLength = bestAudio.itagItem?.contentLength ?: -1L
                } else {
                    val bestVideo = streamExtractor.videoStreams.maxByOrNull { it.bitrate }
                        ?: throw Exception("No playable streams found by NewPipe for $videoId")
                    finalUri = bestVideo.content.toUri()
                    bitrate = bestVideo.bitrate.toLong()
                    itag = bestVideo.itag
                    mimeType = "video/mp4"
                    contentLength = bestVideo.itagItem?.contentLength ?: -1L
                }

                if (contentLength == -1L) {
                    contentLength = fetchContentLength(finalUri!!.toString())
                }
            }.onFailure { e ->
                Log.e("SoundPod-Debug", "NewPipe resolution failed in PreCache for $videoId", e)
                return
            }
        }

        if (finalUri == null) return
        val uri = finalUri!!

        // If we are doing a partial cache, check if already sufficiently cached
        if (length != -1L && cacheManager.isCached(videoId, 0, length)) {
            Log.i("SoundPod-PreCache", "$videoId is already partially cached, skipping.")
            return
        }

        val actualLength = if (length == -1L) contentLength ?: -1L else length
        if (length == -1L && actualLength > 0 && cacheManager.isCached(videoId, 0, actualLength)) {
            Log.i("SoundPod-PreCache", "$videoId is already fully cached, skipping.")
            com.github.soundpod.transaction {
                db.insert(
                    Format(
                        songId = videoId,
                        itag = itag,
                        mimeType = mimeType,
                        bitrate = bitrate,
                        contentLength = actualLength,
                        lastModified = System.currentTimeMillis(),
                        loudnessDb = null
                    )
                )
                db.insert(DownloadedSong(videoId))
                db.deletePrecachedSong(videoId)
            }
            return
        }

        mediaSourceProvider.injectUrl(videoId, uri)

        val dataSpec = DataSpec.Builder()
            .setUri(uri)
            .setKey(videoId)
            .setPosition(0)
            .setLength(actualLength)
            .build()

        val upstreamDataSource = if (playbackSource == PlaybackSource.NewPipe) {
            OkHttpDataSource.Factory(okHttpClient)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .createDataSource()
        } else {
            OkHttpDataSource.Factory(okHttpClient)
                .setUserAgent(PlayerMediaSourceProvider.DEFAULT_USER_AGENT)
                .setDefaultRequestProperties(buildMap {
                    put("Referer", "https://www.youtube.com/")
                    put("Origin", "https://www.youtube.com")
                    put("X-YouTube-Client-Name", "28")
                    put("X-YouTube-Client-Version", "1.71.26")
                    Innertube.visitorData?.let { put("X-Goog-Visitor-Id", it) }
                    Innertube.cookies?.let { put("Cookie", it) }
                })
                .createDataSource()
        }

        val cacheDataSource = CacheDataSource.Factory()
            .setCache(cacheManager.cache)
            .setUpstreamDataSourceFactory { upstreamDataSource }
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cacheManager.cache))
            .createDataSource()

        try {
            Log.d("SoundPod-PreCache", "Starting CacheWriter for $videoId")
            val writer = CacheWriter(cacheDataSource, dataSpec, null) { requestLength, bytesCached, _ ->
                if (length == -1L) {
                    val progress = if (requestLength > 0) (bytesCached * 100 / requestLength).toInt() else 0
                    if (bytesCached % (1024 * 1024) < 1024) { // Log every ~1MB
                        val totalStr = if (requestLength > 0) "${requestLength / 1024 / 1024}MB" else "unknown"
                        Log.d("SoundPod-PreCache", "Download progress for $videoId: $progress% (${bytesCached / 1024 / 1024}MB / $totalStr)")
                    }
                }
            }
            writer.cache()
            
            if (length == -1L) {
                // Update database with the actual cached length for full downloads
                val metadataLength = ContentMetadata.getContentLength(cacheManager.cache.getContentMetadata(videoId))
                val finalLength = if (metadataLength > 0) metadataLength else {
                    cacheManager.cache.getCachedSpans(videoId).sumOf { it.length }
                }

                Log.d("SoundPod-PreCache", "Final calculated length for $videoId: $finalLength bytes")

                if (finalLength > 0) {
                    // Explicitly set the content length metadata in the cache to ensure offline playback works without resolution
                    val mutations = ContentMetadataMutations()
                    ContentMetadataMutations.setContentLength(mutations, finalLength)
                    runCatching { cacheManager.cache.applyContentMetadataMutations(videoId, mutations) }

                    // Use internal.runInTransaction for blocking commit to ensure visibility
                    com.github.soundpod.internal.runInTransaction {
                        db.insert(
                            Format(
                                songId = videoId,
                                itag = itag,
                                mimeType = mimeType,
                                bitrate = bitrate,
                                contentLength = finalLength,
                                lastModified = System.currentTimeMillis(),
                                loudnessDb = null
                            )
                        )
                        db.insert(DownloadedSong(videoId))
                        db.deletePrecachedSong(videoId)
                    }
                } else {
                    Log.w("SoundPod-PreCache", "Downloaded song $videoId has 0 length. Not marking as Downloaded.")
                }
                
                withContext(Dispatchers.Main) {
                    MainApplication.appContext.toast("Successfully downloaded ${mediaItem?.mediaMetadata?.title ?: videoId}")
                }
            } else {
                // Partial cache, mark as temp for 24h cleanup
                db.insert(PrecachedSong(videoId))
            }
            Log.i("SoundPod-PreCache", "Successfully cached $videoId")
        } catch (e: Exception) {
            if (e.message?.contains("403") == true) {
                Log.w("SoundPod-PreCache", "Hit 403 for $videoId, evicting URL and failing.")
                mediaSourceProvider.evictUrl(videoId)
            }
            Log.e("SoundPod-PreCache", "Caching failed for $videoId: ${e.message}")
            if (length == -1L) {
                withContext(Dispatchers.Main) {
                    MainApplication.appContext.toast("Download failed for ${mediaItem?.mediaMetadata?.title ?: videoId}: ${e.message}")
                }
            }
        }
    }

    private suspend fun fetchContentLength(url: String): Long = withContext(Dispatchers.IO) {
        val request = okhttp3.Request.Builder()
            .url(url)
            .head()
            .build()
        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.header("Content-Length")?.toLongOrNull() ?: -1L
                } else -1L
            }
        }.getOrDefault(-1L)
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
