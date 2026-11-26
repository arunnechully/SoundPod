package com.github.soundpod.ui.screens.playlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.github.innertube.Innertube
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
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


@ExperimentalAnimationApi
@UnstableApi
@Composable
fun OnlinePlaylistSongs(
    playlistPage: Innertube.PlaylistOrAlbumPage?,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    val blacklistedSongIds by remember { db.blacklistedSongIds() }.collectAsState(initial = emptyList())
    val blacklistedSet = remember(blacklistedSongIds) { blacklistedSongIds.toSet() }

    val songs = (playlistPage?.songsPage?.items ?: emptyList()).filterNot { it.key in blacklistedSet }
    val continuation = playlistPage?.songsPage?.continuation

    androidx.compose.runtime.LaunchedEffect(playlistPage) {
        songs.take(5).map(Innertube.SongItem::asMediaItem).let { mediaItems ->
            binder?.preCacheManager?.preCache(mediaItems)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp + playerPadding),
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(
            items = songs,
            key = { index, song -> "${song.key}_$index" }
        ) { index, song ->
            if (index >= songs.size - 5 && continuation != null && !isLoadingMore) {
                androidx.compose.runtime.LaunchedEffect(index, continuation) {
                    onLoadMore()
                }
            }

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
                        songs.map(Innertube.SongItem::asMediaItem).let { mediaItems ->
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
        } else if (isLoadingMore) {
            item(key = "loading_more") {
                ShimmerHost {
                    repeat(3) {
                        ListItemPlaceholder()
                    }
                }
            }
        }
    }
}
