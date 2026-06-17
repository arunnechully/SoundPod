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
import com.github.soundpod.models.Artist
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.items.LocalArtistItem

@Composable
fun FavoritesArtist(
    artists: List<Artist>,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    val playerPadding = LocalPlayerPadding.current

    SettingsScreenLayout(
        title = stringResource(R.string.favorite_artists),
        onBackClick = onBackClick,
        scrollable = false,
        horizontalPadding = 0.dp
    ) {
        SettingsCard {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = playerPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = artists,
                    key = { it.id }
                ) { artist ->
                    LocalArtistItem(
                        artist = artist,
                        onClick = { onArtistClick(artist.id) }
                    )
                }
            }
        }
    }
}
