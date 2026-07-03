package com.github.soundpod.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.innertube.Innertube
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.appearance.LoadingAnimation
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.items.AlbumItem
import com.github.soundpod.ui.items.ArtistItem
import com.github.soundpod.ui.items.PlaylistItem
import com.github.soundpod.ui.items.SongItem
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlay

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@UnstableApi
@Composable
fun OnlineSearch(
    songResults: List<Innertube.SongItem>?,
    albumResults: List<Innertube.AlbumItem>?,
    artistResults: List<Innertube.ArtistItem>?,
    playlistResults: List<Innertube.PlaylistItem>?,
    isLoading: Boolean,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onViewAllClick: (String) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    androidx.compose.runtime.LaunchedEffect(songResults) {
        songResults?.take(5)?.map { it.key }?.let { videoIds ->
            binder?.preCacheManager?.preCache(videoIds)
        }
    }

    if (isLoading) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            LoadingAnimation(
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Songs Section
            if (songResults?.isNotEmpty() == true) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    SearchSectionHeader(
                        title = stringResource(R.string.songs),
                        onViewAllClick = onViewAllClick,
                        colorPalette = colorPalette
                    )
                }
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    SettingsCard {
                        Column {
                            songResults.forEach { song ->
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
                        }
                    }
                }
            }

            // Albums Section
            if (albumResults?.isNotEmpty() == true) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    SearchSectionHeader(
                        title = stringResource(R.string.albums),
                        onViewAllClick = onViewAllClick,
                        colorPalette = colorPalette
                    )
                }
                items(albumResults.take(6)) { album ->
                    AlbumItem(
                        album = album,
                        onClick = { onAlbumClick(album.key) }
                    )
                }
            }

            // Artists Section
            if (artistResults?.isNotEmpty() == true) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    SearchSectionHeader(
                        title = stringResource(R.string.artists),
                        onViewAllClick = onViewAllClick,
                        colorPalette = colorPalette
                    )
                }
                items(artistResults.take(6)) { artist ->
                    ArtistItem(
                        artist = artist,
                        onClick = { onArtistClick(artist.key) }
                    )
                }
            }

            // Playlists Section
            if (playlistResults?.isNotEmpty() == true) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    SearchSectionHeader(
                        title = stringResource(R.string.playlists),
                        onViewAllClick = onViewAllClick,
                        colorPalette = colorPalette
                    )
                }
                items(playlistResults.take(6)) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    onViewAllClick: (String) -> Unit,
    colorPalette: com.github.core.ui.ColorPalette,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 22.dp, end = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = colorPalette.text.copy(alpha = 0.5f)
        )

        TextButton(
            onClick = { onViewAllClick(title) },
            colors = ButtonDefaults.textButtonColors(contentColor = colorPalette.text)
        ) {
            Text(
                text = stringResource(R.string.view_all),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
