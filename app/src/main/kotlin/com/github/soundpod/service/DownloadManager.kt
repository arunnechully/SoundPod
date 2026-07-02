package com.github.soundpod.service

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.db
import com.github.soundpod.enums.DownloadDiskCacheMaxSize
import com.github.soundpod.extractor.youtube.YoutubeStreamExtractor
import com.github.soundpod.models.Album
import com.github.soundpod.models.Artist
import com.github.soundpod.models.DownloadedSong
import com.github.soundpod.models.Format
import com.github.soundpod.models.Song
import com.github.soundpod.models.SongAlbumMap
import com.github.soundpod.models.SongArtistMap
import com.github.soundpod.utils.downloadDiskCacheMaxSizeKey
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.thumbnail
import com.github.soundpod.utils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
class DownloadManager(
    private val context: android.content.Context,
    private val cacheManager: PlayerCacheManager,
    private val mediaSourceProvider: PlayerMediaSourceProvider
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = ConcurrentHashMap<String, Job>()

    private val _downloadedSize = MutableStateFlow(0L)
    val downloadedSize = _downloadedSize.asStateFlow()

    init {
        updateDownloadedSize()
    }

    fun updateDownloadedSize() {
        scope.launch {
            db.downloadedSongsWithContentLength().first().let { songs ->
                val totalSize = songs.sumOf { it.contentLength ?: 0L }
                _downloadedSize.update { totalSize }
            }
        }
    }

    fun download(mediaItem: MediaItem) {
        val videoId = mediaItem.mediaId
        if (videoId.isBlank()) return
        if (activeDownloads.containsKey(videoId)) return

        val job = scope.launch {
            try {
                // 0. Check if already downloaded or cached
                if (db.downloadedSong(videoId).first() != null) {
                    withContext(Dispatchers.Main) {
                        context.toast("Already downloaded")
                    }
                    return@launch
                }
                
                // If it's already in the regular cache, we might still want to "promote" it to download cache
                // to make it permanent, so we proceed with download logic which uses CacheWriter.
                // CacheWriter is smart enough to not re-download if data is already present in the TARGET cache.
                // But here our target is downloadCache.

                withContext(Dispatchers.Main) {
                    context.toast("Downloading ${mediaItem.mediaMetadata.title ?: "song"}...")
                }
                Log.d("SoundPod-Download", "Starting download for $videoId")

                // 0. Check Max Size Limit
                val maxSizeEnum = context.preferences.getEnum(downloadDiskCacheMaxSizeKey, DownloadDiskCacheMaxSize.`2GB`)
                if (maxSizeEnum != DownloadDiskCacheMaxSize.Unlimited) {
                    val currentSize = db.downloadedSongsWithContentLength().first().sumOf { it.contentLength ?: 0L }
                    if (currentSize >= maxSizeEnum.bytes) {
                        Log.w("SoundPod-Download", "Download limit reached ($currentSize bytes), skipping download.")
                        withContext(Dispatchers.Main) {
                            context.toast("Download limit reached!")
                        }
                        return@launch
                    }
                }
                
                // 1. Resolve Highest Quality URL via NewPipe (prefers Opus) + Innertube fallback
                val streamExtractor = YoutubeStreamExtractor("https://www.youtube.com/watch?v=$videoId", null)
                streamExtractor.fetchPage()
                val bestStream = streamExtractor.audioStreams.maxByOrNull { it.averageBitrate }
                
                val resolvedStream = bestStream ?: run {
                    val playerResponse = Innertube.player(videoId)?.getOrNull()
                    val fallbackExtractor = YoutubeStreamExtractor("https://www.youtube.com/watch?v=$videoId", playerResponse)
                    fallbackExtractor.fetchPage()
                    fallbackExtractor.audioStreams.maxByOrNull { it.averageBitrate }
                }

                val uri = resolvedStream?.content?.toUri() ?: run {
                    Log.e("SoundPod-Download", "Failed to resolve download URL for $videoId")
                    withContext(Dispatchers.Main) {
                        context.toast("Failed to resolve download URL")
                    }
                    return@launch
                }
                
                // 2. Fetch Thumbnail for Offline
                val localThumbnailUrl = downloadThumbnail(videoId, mediaItem.mediaMetadata.artworkUri?.toString())

                // 3. Persist Metadata
                Log.d("SoundPod-Download", "Persisting metadata for $videoId")
                persistMetadata(mediaItem, resolvedStream, localThumbnailUrl)

                // 4. Download Full Content
                Log.d("SoundPod-Download", "Starting CacheWriter for $videoId")
                val length = resolvedStream.contentLength ?: -1L
                val dataSpec = DataSpec.Builder()
                    .setUri(uri)
                    .setKey(videoId)
                    .setPosition(0L)
                    .setLength(length)
                    .build()

                val upstreamDataSource = OkHttpDataSource.Factory(mediaSourceProvider.okHttpClient)
                    .setUserAgent(Innertube.USER_AGENT)
                    .createDataSource()

                val cacheDataSource = CacheDataSource.Factory()
                    .setCache(cacheManager.downloadCache)
                    .setUpstreamDataSourceFactory { upstreamDataSource }
                    .createDataSource()

                CacheWriter(cacheDataSource, dataSpec, null, null).cache()
                
                db.insert(DownloadedSong(videoId))
                updateDownloadedSize()
                Log.i("SoundPod-Download", "Successfully downloaded and persisted $videoId")
                withContext(Dispatchers.Main) {
                    context.toast("Downloaded: ${mediaItem.mediaMetadata.title ?: "song"}")
                }
            } catch (e: Exception) {
                Log.e("SoundPod-Download", "Download failed for $videoId", e)
                withContext(Dispatchers.Main) {
                    val errorMessage = e.localizedMessage ?: "Unknown error"
                    context.toast("Download failed: ${mediaItem.mediaMetadata.title ?: "song"} ($errorMessage)")
                }
            } finally {
                activeDownloads.remove(videoId)
            }
        }
        activeDownloads[videoId] = job
    }
    
    private suspend fun persistMetadata(mediaItem: MediaItem, stream: YoutubeStreamExtractor.Stream, localThumbnailUrl: String?) {
        val metadata = mediaItem.mediaMetadata
        val videoId = mediaItem.mediaId

        val existingSong = db.song(videoId).first()

        // Upsert Song - ensure we don't overwrite likedAt or totalPlayTimeMs
        db.upsert(
            Song(
                id = videoId,
                title = metadata.title?.toString() ?: existingSong?.title ?: "Unknown",
                artistsText = metadata.artist?.toString() ?: existingSong?.artistsText,
                durationText = metadata.extras?.getString("durationText") ?: existingSong?.durationText,
                thumbnailUrl = localThumbnailUrl ?: metadata.artworkUri?.toString() ?: existingSong?.thumbnailUrl,
                likedAt = existingSong?.likedAt,
                totalPlayTimeMs = existingSong?.totalPlayTimeMs ?: 0L
            )
        )

        // Insert/Replace Format
        db.insert(
            Format(
                songId = videoId,
                itag = null,
                mimeType = null,
                bitrate = stream.averageBitrate,
                contentLength = stream.contentLength,
                lastModified = System.currentTimeMillis()
            )
        )

        // Insert Artist if available
        val artistIds = metadata.extras?.getStringArrayList("artistIds")
        val artistNames = metadata.extras?.getStringArrayList("artistNames")
        if (artistIds != null && artistNames != null) {
            val artists = artistIds.zip(artistNames).map { (id, name) ->
                Artist(id = id, name = name, thumbnailUrl = null)
            }
            val maps = artistIds.map { id ->
                SongArtistMap(songId = videoId, artistId = id)
            }
            db.insert(artists, maps)
        }

        // Upsert Album if available
        val albumId = metadata.extras?.getString("albumId")
        val albumTitle = metadata.albumTitle?.toString()
        if (albumId != null && albumTitle != null) {
            db.upsert(
                Album(id = albumId, title = albumTitle, thumbnailUrl = metadata.artworkUri?.toString())
            )
            db.insert(
                SongAlbumMap(songId = videoId, albumId = albumId, position = null)
            )
        }
    }

    private suspend fun downloadThumbnail(videoId: String, thumbnailUrl: String?): String? {
        val highResUrl = thumbnailUrl.thumbnail(1024) ?: return null
        val folder = context.filesDir.resolve("thumbnails").also { if (!it.exists()) it.mkdirs() }
        val file = folder.resolve("$videoId.webp")

        return withContext(Dispatchers.IO) {
            try {
                val request = okhttp3.Request.Builder().url(highResUrl).build()
                mediaSourceProvider.okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bytes = response.body.bytes()
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            java.io.FileOutputStream(file).use { out ->
                                @Suppress("DEPRECATION")
                                val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
                                } else {
                                    android.graphics.Bitmap.CompressFormat.WEBP
                                }
                                bitmap.compress(format, 90, out)
                            }
                            "file://${file.absolutePath}"
                        } else null
                    } else null
                }
            } catch (e: Exception) {
                Log.e("SoundPod-Download", "Thumbnail download failed for $videoId", e)
                null
            }
        }
    }

    fun cancelDownload(videoId: String) {
        activeDownloads[videoId]?.cancel()
        activeDownloads.remove(videoId)
    }

    fun isDownloaded(videoId: String): Flow<Boolean> = db.downloadedSong(videoId).map { it != null }
}
