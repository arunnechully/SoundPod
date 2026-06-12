package com.github.soundpod.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.innertube.Innertube
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.ActionInfo
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.ShimmerHost
import com.github.soundpod.ui.components.SwipeToActionBox
import com.github.soundpod.ui.items.ListItemPlaceholder
import com.github.soundpod.ui.items.SongItem
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.enqueue
import com.github.soundpod.utils.forcePlayAtIndex

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun PlaylistSongs(
    playlistPage: Innertube.PlaylistOrAlbumPage?,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    LazyColumn(
        contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp + playerPadding),
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(
            items = playlistPage?.songsPage?.items ?: emptyList(),
            key = { _, song -> song.key }
        ) { index, song ->
            SwipeToActionBox(
                primaryAction = ActionInfo(
                    onClick = { binder?.player?.enqueue(song.asMediaItem) },
                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    description = R.string.enqueue
                )
            ) {
                SongItem(
                    song = song,
                    onClick = {
                        playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)
                            ?.let { mediaItems ->
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(mediaItems, index)
                            }
                    },
                    onLongClick = {
                        menuState.display {
                            NonQueuedMediaItemMenu(
                                onDismiss = menuState::hide,
                                mediaItem = song.asMediaItem,
                                onGoToAlbum = onGoToAlbum,
                                onGoToArtist = onGoToArtist
                            )
                        }
                    }
                )
            }
        }

        if (playlistPage == null) {
            item(key = "loading") {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceholder()
                    }
                }
            }
        }
    }
}
