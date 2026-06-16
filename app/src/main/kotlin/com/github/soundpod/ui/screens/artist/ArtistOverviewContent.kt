package com.github.soundpod.ui.screens.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.innertube.Innertube
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.ShimmerHost
import com.github.soundpod.ui.components.TextPlaceholder
import com.github.soundpod.ui.items.AlbumItem
import com.github.soundpod.ui.items.ListItemPlaceholder
import com.github.soundpod.ui.items.SongItem
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun ArtistOverviewContent(
    youtubeArtistPage: Innertube.ArtistPage?,
    onAlbumClick: (String) -> Unit,
    playerPadding: Dp,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = playerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (youtubeArtistPage != null) {
            youtubeArtistPage.songs?.let { songs ->
                ArtistSongsSection(
                    songs = songs,
                    onAlbumClick = onAlbumClick
                )
            }

            youtubeArtistPage.albums?.let { albums ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))
                ArtistAlbumsSection(
                    title = stringResource(id = R.string.albums),
                    albums = albums,
                    onAlbumClick = onAlbumClick
                )
            }

            youtubeArtistPage.singles?.let { singles ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))
                ArtistAlbumsSection(
                    title = stringResource(id = R.string.singles),
                    albums = singles,
                    onAlbumClick = onAlbumClick
                )
            }

            youtubeArtistPage.description?.let { description ->
                ArtistDescriptionSection(description = description)
            }
        } else {
            ShimmerHost(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                val placeholderModifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)

                TextPlaceholder(modifier = placeholderModifier)

                repeat(5) {
                    ListItemPlaceholder()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ArtistSongsSection(
    songs: List<Innertube.SongItem>,
    onAlbumClick: (String) -> Unit,
    showTitle: Boolean = true
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    if (showTitle) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.songs),
                style = typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }

    songs.forEachIndexed { index, song ->
        SongItem(
            song = song,
            onClick = {
                binder?.stopRadio()
                binder?.player?.forcePlayAtIndex(songs.map { it.asMediaItem }, index)
            },
            onLongClick = {
                menuState.display {
                    NonQueuedMediaItemMenu(
                        onDismiss = menuState::hide,
                        mediaItem = song.asMediaItem,
                        onGoToAlbum = onAlbumClick
                    )
                }
            }
        )
    }
}

@Composable
internal fun ArtistAlbumsSection(
    title: String,
    albums: List<Innertube.AlbumItem>,
    onAlbumClick: (String) -> Unit,
    showTitle: Boolean = true
) {
    val itemSize = 140.dp

    if (showTitle) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(
            items = albums.filter { it.key.isNotEmpty() }.distinctBy { it.key },
            key = Innertube.AlbumItem::key
        ) { album ->
            AlbumItem(
                modifier = Modifier.widthIn(max = itemSize),
                album = album,
                onClick = { onAlbumClick(album.key) }
            )
        }
    }
}

@Composable
internal fun ArtistDescriptionSection(description: String) {
    val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

    Spacer(modifier = Modifier.height(Dimensions.spacer))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.about),
            style = typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    Text(
        text = if (attributionsIndex == -1) {
            description
        } else {
            description.substring(0, attributionsIndex)
        },
        style = typography.bodySmall,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .alpha(Dimensions.MEDIUMOPACITY)
    )

    if (attributionsIndex != -1) {
        Text(
            text = "From Wikipedia under Creative Commons Attribution CC-BY-SA 3.0",
            style = typography.bodySmall,
            modifier = Modifier
                .alpha(Dimensions.MEDIUMOPACITY)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
        )
    }
}
