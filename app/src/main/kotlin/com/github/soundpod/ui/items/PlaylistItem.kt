package com.github.soundpod.ui.items

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import coil3.compose.AsyncImage
import com.github.innertube.Innertube
import com.github.soundpod.Database
import com.github.soundpod.R
import com.github.soundpod.models.PlaylistPreview
import com.github.soundpod.ui.styling.px
import com.github.soundpod.utils.thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun PlaylistItem(
    modifier: Modifier = Modifier,
    playlist: Innertube.PlaylistItem,
    onClick: () -> Unit
) {
    ItemContainer(
        modifier = modifier,
        title = playlist.info?.name ?: "",
        subtitle = playlist.channel?.name,
        onClick = onClick
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = playlist.thumbnail?.url.thumbnail(maxWidth.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(maxWidth)
                    .clip(MaterialTheme.shapes.large)
            )
        }
    }
}

@Composable
fun BuiltInPlaylistItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    name: String,
    onClick: () -> Unit
) {
    ItemContainer(
        modifier = modifier,
        title = name,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LocalPlaylistItem(
    modifier: Modifier = Modifier,
    playlist: PlaylistPreview,
    onClick: () -> Unit
) {
    ItemContainer(
        modifier = modifier,
        title = playlist.playlist.name,
        subtitle = pluralStringResource(
            id = R.plurals.number_of_songs,
            count = playlist.songCount,
            playlist.songCount
        ),
        onClick = onClick
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1F)
        ) {
            val thumbnailWidthDp = maxWidth
            val thumbnailWidthPx = maxWidth.px

            val thumbnails by remember {
                Database.playlistThumbnailUrls(playlist.playlist.id).distinctUntilChanged().map {
                    it.map { url ->
                        url.thumbnail(size = thumbnailWidthPx / 2)
                    }
                }
            }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

            if (thumbnails.toSet().size == 1) {
                AsyncImage(
                    model = thumbnails.first().thumbnail(thumbnailWidthPx),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.clip(MaterialTheme.shapes.large)
                )
            } else {
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).forEachIndexed { index, alignment ->
                    AsyncImage(
                        model = thumbnails.getOrNull(index),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(alignment)
                            .size(thumbnailWidthDp / 2)
                    )
                }
            }
        }
    }
}