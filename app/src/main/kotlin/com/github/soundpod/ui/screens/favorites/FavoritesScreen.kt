package com.github.soundpod.ui.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayFromBeginning
import com.github.soundpod.viewmodels.favorites.FavoritesViewModel

@Composable
fun FavoritesScreen(
    onBackClick: () -> Unit = {},
    onFavoriteTracksClick: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    isEmbedded: Boolean = false
) {
    val binder = LocalPlayerServiceBinder.current
    val viewModel: FavoritesViewModel = viewModel()

    val content = @Composable {
        FavoritesMainContent(
            viewModel = viewModel,
            onFavoriteTracksClick = onFavoriteTracksClick,
            onGoToAlbum = onGoToAlbum,
            onGoToArtist = onGoToArtist,
            onPlayTracks = {
                binder?.stopRadio()
                binder?.player?.forcePlayFromBeginning(viewModel.favoriteSongs.map { it.asMediaItem })
            }
        )
    }

    if (isEmbedded) {
        content()
    } else {
        SettingsScreenLayout(
            title = stringResource(R.string.favorites),
            onBackClick = onBackClick,
            scrollable = false,
            horizontalPadding = 0.dp
        ) {
            content()
        }
    }
}

@Composable
fun FavoritesMainContent(
    viewModel: FavoritesViewModel,
    onFavoriteTracksClick: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onPlayTracks: () -> Unit
) {
    val noFavorites = viewModel.favoriteSongs.isEmpty() &&
            viewModel.favoriteAlbums.isEmpty() &&
            viewModel.favoriteArtists.isEmpty()

    if (noFavorites) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = "file:///android_asset/img/A3.webp",
                contentDescription = null,
                modifier = Modifier
                    .size(300.dp)
                    .graphicsLayer { alpha = 0.15f }
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.no_favorites),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_favorites_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        val playerPadding = LocalPlayerPadding.current

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(
                top = 8.dp,
                start = 8.dp,
                end = 8.dp,
                bottom = playerPadding + 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (viewModel.favoriteSongs.isNotEmpty()) {
                item {
                    val songCount = viewModel.favoriteSongs.size
                    FavoritesCard(
                        title = stringResource(R.string.favorite_tracks),
                        subtitle = pluralStringResource(id = R.plurals.number_of_songs, count = songCount, songCount),
                        icon = Icons.Default.MusicNote,
                        thumbnailUrls = viewModel.favoriteSongs.mapNotNull { it.thumbnailUrl }.take(1),
                        onClick = onFavoriteTracksClick,
                        onPlayClick = onPlayTracks
                    )
                }
            }

            items(
                items = viewModel.favoriteAlbums,
                key = { "album_${it.id}" }
            ) { album ->
                FavoritesCard(
                    title = album.title ?: "",
                    label = stringResource(R.string.album),
                    icon = Icons.Default.MusicNote, // Fallback if no thumbnail
                    thumbnailUrls = listOfNotNull(album.thumbnailUrl),
                    onClick = { onGoToAlbum(album.id) }
                )
            }

            items(
                items = viewModel.favoriteArtists,
                key = { "artist_${it.id}" }
            ) { artist ->
                FavoritesCard(
                    title = artist.name ?: "",
                    label = stringResource(R.string.artist),
                    icon = Icons.Default.MusicNote, // Fallback
                    thumbnailUrls = listOfNotNull(artist.thumbnailUrl),
                    onClick = { onGoToArtist(artist.id) }
                )
            }
        }
    }
}

