package com.github.soundpod.ui.screens.album

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.width
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
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.db
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Song
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.SongListContent
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

    val (colorPalette) = LocalAppearance.current

    var songs: List<Song> by remember { mutableStateOf(emptyList()) }
    var currentPlayingId by remember { mutableStateOf<String?>(null) }
    val player = binder?.player

    var sortBy by remember { mutableStateOf(SongSortBy.Artist) }
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
    SongListContent(
        songs = songs,
        showThumbnail = false,
        showMoreVert = false,
        currentPlayingId = currentPlayingId,
        leadingContent = { index, isPlaying ->
            val highlightColor = if (isPlaying) {
                colorPalette.accent
            } else {
                colorPalette.text
            }
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
        sortBy = sortBy,
        onSortByChange = { sortBy = it },
        sortByEntries = SongSortBy.entries.toList(),
        onSongClick = {index ->
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(
                songs.map(Song::asMediaItem),
                index
            )
        },
        onSongLongClick = {song ->
            menuState.display {
                NonQueuedMediaItemMenu(
                    onDismiss = menuState::hide,
                    mediaItem = song.asMediaItem,
                    onGoToArtist = onGoToArtist
                )
            }
        },
        onPlayAll = {
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(songs.map(Song::asMediaItem), 0)
        },
        onShuffleAll = {
            binder?.stopRadio()
            val shuffledSongs = songs.shuffled()
            binder?.player?.forcePlayAtIndex(shuffledSongs.map(Song::asMediaItem), 0)
        }
    )
}