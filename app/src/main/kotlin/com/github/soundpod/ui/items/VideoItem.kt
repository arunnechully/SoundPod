package com.github.soundpod.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.innertube.Innertube
import com.github.soundpod.ui.styling.onOverlay
import com.github.soundpod.ui.styling.overlay

@Composable
fun VideoItem(
    video: Innertube.VideoItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ListItemContainer(
        title = video.info?.name ?: "",
        subtitle = if (video.authors.isNullOrEmpty()) {
            video.viewsText ?: ""
        } else {
            "${video.authors?.joinToString(separator = "") { it.name ?: "" }} • ${video.viewsText}"
        },
        onClick = onClick,
        onLongClick = onLongClick,
        maxLines = 2,
        thumbnail = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = video.thumbnail?.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                )

                video.durationText?.let { duration ->
                    Text(
                        text = duration,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onOverlay,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(all = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.overlay,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
        },
        thumbnailHeight = 64.dp,
        thumbnailAspectRatio = 16F / 9F
    )
}