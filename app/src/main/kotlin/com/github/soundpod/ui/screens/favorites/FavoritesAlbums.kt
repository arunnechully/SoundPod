package com.github.soundpod.ui.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.R
import com.github.soundpod.models.Album
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.items.LocalAlbumItem

@Composable
fun FavoritesAlbums(
    albums: List<Album>,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit
) {
    val playerPadding = LocalPlayerPadding.current

    SettingsScreenLayout(
        title = stringResource(R.string.favorite_albums),
        onBackClick = onBackClick,
        scrollable = false,
        horizontalPadding = 0.dp
    ) {
        SettingsCard {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    start = 8.dp,
                    end = 8.dp,
                    bottom = playerPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = albums,
                    key = { it.id }
                ) { album ->
                    LocalAlbumItem(
                        album = album,
                        onClick = { onAlbumClick(album.id) }
                    )
                }
            }
        }
    }
}
