package com.github.soundpod.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.db
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.Song
import com.github.soundpod.ui.components.SongListContent
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex

@ExperimentalAnimationApi
@Composable
fun FavoritePlaylistSongs(
    playlistId: Long,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current

    var sortBy by remember { mutableStateOf(SongSortBy.Title) }
    var sortOrder by remember { mutableStateOf(SortOrder.Ascending) }

    var playlistSongs: List<Song> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(playlistId, sortBy, sortOrder) {
        db.playlistSongs(playlistId).collect { fetchedSongs ->
            val sortedList = when (sortBy) {
                SongSortBy.Title -> fetchedSongs.sortedBy { it.title }
                SongSortBy.Artist -> fetchedSongs.sortedBy { it.artistsText }
                else -> fetchedSongs
            }
            playlistSongs = if (sortOrder.name == "Descending") sortedList.reversed() else sortedList
        }
    }

    SongListContent(
        songs = playlistSongs,
        sortBy = sortBy,
        onSortByChange = { sortBy = it },
        sortByEntries = SongSortBy.entries.toList(),
        onSongClick = { index ->
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(
                playlistSongs.map(Song::asMediaItem),
                index
            )
        },
        onSongLongClick = { /* Handled by SongListContent internal logic for selection */ },
        onPlayAll = {
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(playlistSongs.map(Song::asMediaItem), 0)
        },
        onShuffleAll = {
            binder?.stopRadio()
            val shuffledSongs = playlistSongs.shuffled()
            binder?.player?.forcePlayAtIndex(shuffledSongs.map(Song::asMediaItem), 0)
        },
        onGoToAlbum = onGoToAlbum,
        onGoToArtist = onGoToArtist,
        modifier = Modifier.fillMaxSize()
    )
}
