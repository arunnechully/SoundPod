package com.github.soundpod.utils

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.github.innertube.Innertube
import com.github.innertube.requests.playlistPageContinuation
import com.github.innertube.utils.plus
import com.github.soundpod.models.Song

val Innertube.SongItem.asMediaItem: MediaItem
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString("") { it.name ?: "" })
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    Bundle().apply {
                        album?.endpoint?.browseId?.let { putString("albumId", it) }
                        durationText?.let { putString("durationText", it) }

                        val names = authors?.filter { it.endpoint != null }?.mapNotNull { it.name }
                        if (!names.isNullOrEmpty()) putStringArrayList("artistNames", ArrayList(names))

                        val ids = authors?.mapNotNull { it.endpoint?.browseId }
                        if (!ids.isNullOrEmpty()) putStringArrayList("artistIds", ArrayList(ids))
                    }
                )
                .build()
        )
        .build()

val Innertube.VideoItem.asMediaItem: MediaItem
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString("") { it.name ?: "" })
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    Bundle().apply {
                        durationText?.let { putString("durationText", it) }

                        if (isOfficialMusicVideo) {
                            val names = authors?.filter { it.endpoint != null }?.mapNotNull { it.name }
                            if (!names.isNullOrEmpty()) putStringArrayList("artistNames", ArrayList(names))

                            val ids = authors?.mapNotNull { it.endpoint?.browseId }
                            if (!ids.isNullOrEmpty()) putStringArrayList("artistIds", ArrayList(ids))
                        }
                    }
                )
                .build()
        )
        .build()

val Song.asMediaItem: MediaItem
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    Bundle().apply {
                        durationText?.let { putString("durationText", it) }
                    }
                )
                .build()
        )
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .build()

fun String?.thumbnail(size: Int): String? {
    if (this == null) return null

    if (this.contains("i.ytimg.com")) {
        return this.replace("hqdefault.jpg", "maxresdefault.jpg")
            .replace("mqdefault.jpg", "maxresdefault.jpg")
            .replace("sddefault.jpg", "maxresdefault.jpg")
    }

    val cleanUrl = this.substringBefore("=")
    return "$cleanUrl=w$size-h$size"
}

fun Uri?.thumbnail(size: Int): Uri? {
    return toString().thumbnail(size)?.toUri()
}

fun formatAsDuration(millis: Long) = DateUtils.formatElapsedTime(millis / 1000).removePrefix("0")

suspend fun Result<Innertube.PlaylistOrAlbumPage>.completed(): Result<Innertube.PlaylistOrAlbumPage>? {
    var playlistPage = getOrNull() ?: return null

    while (playlistPage.songsPage?.continuation != null) {
        val continuation = playlistPage.songsPage?.continuation!!
        val otherPlaylistPageResult =
            Innertube.playlistPageContinuation(continuation = continuation) ?: break

        if (otherPlaylistPageResult.isFailure) break

        otherPlaylistPageResult.getOrNull()?.let { otherSongsPage ->
            playlistPage = playlistPage.copy(songsPage = playlistPage.songsPage + otherSongsPage)
        }
    }

    return Result.success(playlistPage)
}

// Removed `isAtLeastAndroid6` entirely since your minSdkVersion is >= 23

inline val isAtLeastAndroid8
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

inline val isAtLeastAndroid12
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

inline val isAtLeastAndroid13
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU