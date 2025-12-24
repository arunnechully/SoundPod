package com.github.soundpod.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.innertube.Innertube
import com.github.innertube.requests.searchPage
import com.github.innertube.utils.from
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.ActionInfo
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwipeToActionBox
import com.github.soundpod.ui.items.AlbumItem
import com.github.soundpod.ui.items.ArtistItem
import com.github.soundpod.ui.items.ItemPlaceholder
import com.github.soundpod.ui.items.ListItemPlaceholder
import com.github.soundpod.ui.items.PlaylistItem
import com.github.soundpod.ui.items.SongItem
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.enqueue
import com.github.soundpod.utils.forcePlay

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun NewSearchResult(
    navController: NavController,
    query: String,
    resultType: String?
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val emptyItemsText = stringResource(id = R.string.no_results_found)

    val onAlbumClick: (String) -> Unit = { /* navigate */ }
    val onArtistClick: (String) -> Unit = { /* navigate */ }
    val onPlaylistClick: (String) -> Unit = { /* navigate */ }

    SettingsScreenLayout(
        title = resultType ?: "Results",
        onBackClick = { navController.popBackStack() },
        scrollable = false,
        horizontalPadding = 0.dp
    ) {
        SettingsCard {
            when (resultType) {
                "Songs" -> {
                    ItemsPage(
                        tag = "searchResults/$query/songs",
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                Innertube.searchPage(
                                    query = query,
                                    params = Innertube.SearchFilter.Song.value,
                                    fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                )
                            } else {
                                Innertube.searchPage(
                                    continuation = continuation,
                                    fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        itemContent = { song ->
                            SwipeToActionBox(
                                modifier = Modifier.animateItem(),
                                primaryAction = ActionInfo(
                                    onClick = { binder?.player?.enqueue(song.asMediaItem) },
                                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                    description = R.string.enqueue
                                )
                            ) {
                                SongItem(
                                    song = song,
                                    onClick = {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlay(song.asMediaItem)
                                        binder?.setupRadio(song.info?.endpoint)
                                    },
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = song.asMediaItem,
                                                onGoToAlbum = onAlbumClick,
                                                onGoToArtist = onArtistClick
                                            )
                                        }
                                    }
                                )
                            }
                        },
                        itemPlaceholderContent = { ListItemPlaceholder() }
                    )
                }

                "Albums" -> {
                    ItemsPage(
                        tag = "searchResults/$query/albums",
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                Innertube.searchPage(
                                    query = query,
                                    params = Innertube.SearchFilter.Album.value,
                                    fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                )
                            } else {
                                Innertube.searchPage(
                                    continuation = continuation,
                                    fromMusicShelfRendererContent = Innertube.AlbumItem::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        itemContent = { album ->
                            AlbumItem(
                                album = album,
                                onClick = { onAlbumClick(album.key) }
                            )
                        },
                        itemPlaceholderContent = { ItemPlaceholder() }
                    )
                }

                "Artists" -> {
                    ItemsPage(
                        tag = "searchResults/$query/artists",
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                Innertube.searchPage(
                                    query = query,
                                    params = Innertube.SearchFilter.Artist.value,
                                    fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                )
                            } else {
                                Innertube.searchPage(
                                    continuation = continuation,
                                    fromMusicShelfRendererContent = Innertube.ArtistItem::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        itemContent = { artist ->
                            ArtistItem(
                                artist = artist,
                                onClick = { onArtistClick(artist.key) }
                            )
                        },
                        itemPlaceholderContent = { ItemPlaceholder(shape = CircleShape) }
                    )
                }

                "Playlists" -> {
                    ItemsPage(
                        tag = "searchResults/$query/playlists",
                        itemsPageProvider = { continuation ->
                            if (continuation == null) {
                                Innertube.searchPage(
                                    query = query,
                                    params = Innertube.SearchFilter.CommunityPlaylist.value,
                                    fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                )
                            } else {
                                Innertube.searchPage(
                                    continuation = continuation,
                                    fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                                )
                            }
                        },
                        emptyItemsText = emptyItemsText,
                        itemContent = { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist.key) }
                            )
                        },
                        itemPlaceholderContent = { ItemPlaceholder() }
                    )
                }

                else -> {
                    Text(
                        text = "Unknown category: $resultType",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}