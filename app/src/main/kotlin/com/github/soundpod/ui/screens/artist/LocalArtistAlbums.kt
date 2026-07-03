package com.github.soundpod.ui.screens.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.db
import com.github.soundpod.models.Album
import com.github.soundpod.ui.items.LocalAlbumItem

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun LocalArtistAlbums(
    browseId: String,
    onAlbumClick: (String) -> Unit
) {
    val playerPadding = LocalPlayerPadding.current
    var albums by remember { mutableStateOf(emptyList<Album>()) }

    LaunchedEffect(browseId) {
        db.artistAlbums(browseId).collect {
            albums = it
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            bottom = 16.dp + playerPadding
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = albums,
            key = { it.id }
        ) { album ->
            LocalAlbumItem(
                modifier = Modifier.animateItem(),
                album = album,
                onClick = { onAlbumClick(album.id) }
            )
        }
    }
}
