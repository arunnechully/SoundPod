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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.soundpod.R
import com.github.soundpod.viewmodels.favorites.FavoritesViewModel

@Composable
fun FavoritesScreen(
    onFavoriteSongsClick: () -> Unit,
    onFavoriteAlbumsClick: () -> Unit,
    onFavoriteArtistsClick: () -> Unit,
) {
    val viewModel: FavoritesViewModel = viewModel()

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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (viewModel.favoriteSongs.isNotEmpty()) {
                item {
                    FavoritesCard(
                        title = stringResource(R.string.favorite_tracks),
                        icon = Icons.Default.MusicNote,
                        onClick = onFavoriteSongsClick
                    )
                }
            }
            if (viewModel.favoriteAlbums.isNotEmpty()) {
                item {
                    FavoritesCard(
                        title = stringResource(R.string.favorite_albums),
                        icon = Icons.Default.Album,
                        onClick = onFavoriteAlbumsClick
                    )
                }
            }
            if (viewModel.favoriteArtists.isNotEmpty()) {
                item {
                    FavoritesCard(
                        title = stringResource(R.string.favorite_artists),
                        icon = Icons.Default.Person,
                        onClick = onFavoriteArtistsClick
                    )
                }
            }
        }
    }
}
