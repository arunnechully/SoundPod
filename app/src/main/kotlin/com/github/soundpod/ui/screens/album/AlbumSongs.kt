package com.github.soundpod.ui.screens.album

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.db
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Song
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.SortingHeader // Added for SortingHeader
import com.github.soundpod.ui.items.LocalSongItem
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex

@OptIn(ExperimentalAnimationApi::class)
@UnstableApi
@Composable
fun AlbumSongs(
    browseId: String,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    val (colorPalette) = LocalAppearance.current

    var songs: List<Song> by remember { mutableStateOf(emptyList()) }
    var currentPlayingId by remember { mutableStateOf<String?>(null) }
    val player = binder?.player

    var sortBy by remember { mutableStateOf(SongSortBy.Title) }
    var sortOrder by remember { mutableStateOf(SortOrder.Ascending) }

    LaunchedEffect(browseId, sortBy, sortOrder) {
        db.albumSongs(browseId).collect { fetchedSongs ->
            val sortedList = when (sortBy) {
                SongSortBy.Title -> fetchedSongs.sortedBy { it.title }
                SongSortBy.Artist -> fetchedSongs.sortedBy { it.artistsText }
                else -> fetchedSongs
            }
            songs = if (sortOrder.name == "Descending") sortedList.reversed() else sortedList
            
            if (songs.isNotEmpty()) {
                binder?.preCacheManager?.preCache(songs.take(5).map { it.id })
            }
        }
    }
    DisposableEffect(player) {
        currentPlayingId = player?.currentMediaItem?.mediaId

        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentPlayingId = mediaItem?.mediaId
            }
        }
        player?.addListener(listener)

        onDispose {
            player?.removeListener(listener)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp + playerPadding),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header") {
            SortingHeader(
                sortBy = sortBy,
                changeSortBy = { sortBy = it },
                sortByEntries = SongSortBy.entries.toList(),
                sortOrder = sortOrder,
                toggleSortOrder = {
                    sortOrder = if (sortOrder.name == "Ascending") SortOrder.Descending else SortOrder.Ascending
                },
                size = songs.size,
                onPlayClick = {
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(songs.map(Song::asMediaItem), 0)
                },
                onShuffleClick = {
                    binder?.stopRadio()
                    val shuffledSongs = songs.shuffled()
                    binder?.player?.forcePlayAtIndex(shuffledSongs.map(Song::asMediaItem), 0)
                }
            )
        }
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id }
        ) { index, song ->

            val isPlaying = song.id == currentPlayingId

            val highlightColor = if (isPlaying) {
                colorPalette.accent
            } else {
                colorPalette.text
            }

            LocalSongItem(
                song = song,
                showThumbnail = false,
                titleColor = highlightColor,
                showMoreVert = false,
                leadingContent = {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = highlightColor,
                        modifier = Modifier
                            .width(28.dp)
                            .alpha(if (isPlaying) 1f else Dimensions.MEDIUMOPACITY)
                    )
                },
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
                            onGoToArtist = onGoToArtist
                        )
                    }
                }

            )
        }
    }
}