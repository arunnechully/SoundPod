package com.github.musicyou.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.musicyou.Database
import com.github.musicyou.ui.styling.px
import com.github.musicyou.utils.thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun PlaylistThumbnail(
    playlistId: Long
) {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val thumbnailSizeDp = maxWidth - 64.dp
        val thumbnailSizePx = thumbnailSizeDp.px

        val thumbnails by remember {
            Database.playlistThumbnailUrls(playlistId).distinctUntilChanged().map {
                it.map { url ->
                    url.thumbnail(thumbnailSizePx / 2)
                }
            }
        }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

        val modifier = Modifier
            .padding(16.dp)
            .clip(MaterialTheme.shapes.large)
            .size(thumbnailSizeDp)

        if (thumbnails.toSet().size == 1) {
            AsyncImage(
                model = thumbnails.first().thumbnail(thumbnailSizePx),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
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
                            .size(thumbnailSizeDp / 2)
                    )
                }
            }
        }
    }
}