package com.github.soundpod.utils

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.github.soundpod.models.Album
import com.github.soundpod.models.Artist
import com.github.soundpod.models.Song
import com.github.soundpod.models.SongAlbumMap
import com.github.soundpod.models.SongArtistMap

data class MediaStoreResult(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val songAlbumMaps: List<SongAlbumMap>,
    val songArtistMaps: List<SongArtistMap>
)

fun Context.queryMediaStoreSongs(): MediaStoreResult {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        return MediaStoreResult(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }

    val songs = mutableListOf<Song>()
    val albums = mutableMapOf<String, Album>()
    val artists = mutableMapOf<String, Artist>()
    val songAlbumMaps = mutableListOf<SongAlbumMap>()
    val songArtistMaps = mutableListOf<SongArtistMap>()

    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.ARTIST_ID
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    contentResolver.query(
        collection,
        projection,
        selection,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val title = cursor.getString(titleColumn)
            val artistName = cursor.getString(artistColumn)
            val albumName = cursor.getString(albumColumn)
            val duration = cursor.getLong(durationColumn)
            val albumId = cursor.getLong(albumIdColumn)
            val artistId = cursor.getLong(artistIdColumn)

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                id
            )

            val artworkUri = ContentUris.withAppendedId(
                "content://media/external/audio/albumart".toUri(),
                albumId
            )

            val songId = contentUri.toString()
            val localAlbumId = "local_album:$albumId"
            val localArtistId = "local_artist:$artistId"

            songs.add(
                Song(
                    id = songId,
                    title = title,
                    artistsText = artistName,
                    durationText = formatAsDuration(duration),
                    thumbnailUrl = artworkUri.toString()
                )
            )

            if (!artists.containsKey(localArtistId)) {
                artists[localArtistId] = Artist(
                    id = localArtistId,
                    name = artistName,
                    thumbnailUrl = null // MediaStore doesn't easily provide artist art
                )
            }
            songArtistMaps.add(SongArtistMap(songId = songId, artistId = localArtistId))

            if (!albums.containsKey(localAlbumId)) {
                albums[localAlbumId] = Album(
                    id = localAlbumId,
                    title = albumName,
                    thumbnailUrl = artworkUri.toString(),
                    authorsText = artistName
                )
            }
            songAlbumMaps.add(SongAlbumMap(songId = songId, albumId = localAlbumId, position = null))
        }
    }
    return MediaStoreResult(songs, albums.values.toList(), artists.values.toList(), songAlbumMaps, songArtistMaps)
}
