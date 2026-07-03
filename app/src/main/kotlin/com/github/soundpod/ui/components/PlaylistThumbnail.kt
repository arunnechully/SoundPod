package com.github.soundpod.ui.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.utils.thumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun PlaylistThumbnail(
    playlistId: Long,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        val thumbnailSizeDp = maxWidth - 64.dp

        val thumbnails by remember {
            db.playlistThumbnailUrls(playlistId).distinctUntilChanged().map {
                it.map { url ->
                    url.thumbnail(1024)
                }
            }
        }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

        val modifier = Modifier
            .padding(16.dp)
            .clip(MaterialTheme.shapes.large)
            .size(thumbnailSizeDp)
            .background(MaterialTheme.colorScheme.surfaceVariant)

        if (thumbnails.isEmpty()) {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(thumbnailSizeDp / 2),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else if (thumbnails.toSet().size == 1) {
            AsyncImage(
                model = thumbnails.first().thumbnail(1024),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                fallback = painterResource(id = R.drawable.app_icon),
                error = painterResource(id = R.drawable.app_icon),
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier
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
                        fallback = painterResource(id = R.drawable.app_icon),
                        error = painterResource(id = R.drawable.app_icon),
                        modifier = Modifier
                            .align(alignment)
                            .size(thumbnailSizeDp / 2)
                    )
                }
            }
        }
    }
}