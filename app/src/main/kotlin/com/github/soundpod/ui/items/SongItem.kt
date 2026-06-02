package com.github.soundpod.ui.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.github.innertube.Innertube
import com.github.soundpod.R
import com.github.soundpod.models.Song
import com.github.soundpod.ui.styling.px
import com.github.soundpod.utils.thumbnail

@Composable
fun SongItem(
    modifier: Modifier = Modifier,
    song: Innertube.SongItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showThumbnail: Boolean = true,
    thumbnailContent: @Composable (() -> Unit)? = null,
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItemContainer(
        modifier = modifier,
        title = song.info?.name ?: "",
        subtitle = song.authors?.mapNotNull { it.name }?.joinToString(" • "),
        onClick = onClick,
        onLongClick = onLongClick,
        thumbnail = if (showThumbnail) {
            { size ->
                Box(contentAlignment = Alignment.Center) {
                    if (thumbnailContent == null) {
                        Icon(
                            painter = painterResource(R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.size(size / 2),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        AsyncImage(
                            model = song.thumbnail?.size(size.px),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                        )
                        onThumbnailContent?.invoke(this)
                    } else {
                        thumbnailContent()
                    }
                }
            }
        } else null,
        trailingContent = trailingContent
    )
}

@Composable
fun LocalSongItem(
    modifier: Modifier = Modifier,
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showThumbnail: Boolean = true,
    showMoreVert: Boolean = true,
    thumbnailContent: @Composable (() -> Unit)? = null,
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItemContainer(
        modifier = modifier,
        title = song.title,
        subtitle = listOfNotNull(song.artistsText, song.durationText).joinToString(" • "),
        onClick = onClick,
        onLongClick = onLongClick,
        thumbnail = if (showThumbnail) {
            { size ->
                Box(contentAlignment = Alignment.Center) {
                    if (thumbnailContent == null) {
                        Icon(
                            painter = painterResource(R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.size(size / 2),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        AsyncImage(
                            model = song.thumbnailUrl?.thumbnail(size.px),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                        )
                        onThumbnailContent?.invoke(this)
                    } else {
                        thumbnailContent()
                    }
                }
            }
        } else null,
        showMoreVert = showMoreVert,
        trailingContent = trailingContent
    )
}
