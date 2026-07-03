package com.github.soundpod.ui.screens.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.db
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Song
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.SortingHeader
import com.github.soundpod.ui.items.LocalSongItem
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex

@OptIn(ExperimentalAnimationApi::class)
@UnstableApi
@Composable
fun LocalArtistSongs(
    browseId: String,
    onGoToAlbum: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    var songs: List<Song> by remember { mutableStateOf(emptyList()) }
    var sortBy by remember { mutableStateOf(SongSortBy.Title) }
    var sortOrder by remember { mutableStateOf(SortOrder.Ascending) }

    LaunchedEffect(browseId, sortBy, sortOrder) {
        db.artistSongs(browseId).collect { fetchedSongs ->
            val sortedList = when (sortBy) {
                SongSortBy.Title -> fetchedSongs.sortedBy { it.title.lowercase() }
                else -> fetchedSongs
            }
            songs = if (sortOrder == SortOrder.Descending) sortedList.reversed() else sortedList
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp + playerPadding),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header") {
            SortingHeader(
                sortBy = sortBy,
                changeSortBy = { sortBy = it },
                sortByEntries = listOf(SongSortBy.Title),
                sortOrder = sortOrder,
                toggleSortOrder = {
                    sortOrder = if (sortOrder == SortOrder.Ascending) SortOrder.Descending else SortOrder.Ascending
                },
                size = songs.size,
                onPlayClick = {
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(songs.map(Song::asMediaItem), 0)
                },
                onShuffleClick = {
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(songs.shuffled().map(Song::asMediaItem), 0)
                }
            )
        }
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id }
        ) { index, song ->
            LocalSongItem(
                song = song,
                onClick = {
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(
                        songs.map(Song::asMediaItem),
                        index
                    )
                },
                onLongClick = {
                    menuState.display {
                        NonQueuedMediaItemMenu(
                            onDismiss = menuState::hide,
                            mediaItem = song.asMediaItem,
                            onGoToAlbum = onGoToAlbum
                        )
                    }
                }
            )
        }
    }
}
