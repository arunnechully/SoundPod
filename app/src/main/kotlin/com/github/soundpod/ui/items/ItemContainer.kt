package com.github.soundpod.ui.items

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import coil3.compose.AsyncImage
import com.github.innertube.Innertube
import com.github.soundpod.R
import com.github.soundpod.models.Song
import com.github.soundpod.ui.components.TextPlaceholder
import com.github.soundpod.ui.styling.px
import com.github.soundpod.ui.styling.shimmer
import com.github.soundpod.utils.thumbnail

@Composable
fun ItemContainer(
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    textAlign: TextAlign = TextAlign.Start,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbnail: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .widthIn(max = 200.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(
                enabled = onClick != null,
                onClick = onClick ?: {}
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1F)
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            thumbnail()
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isPlaceholder) {
            TextPlaceholder()
        } else {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isPlaceholder) {
            TextPlaceholder()
        } else {
            subtitle?.let {
                Text(
                    text = subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ItemPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large
) {
    ItemContainer(
        modifier = modifier,
        isPlaceholder = true,
        title = "",
        shape = shape,
        color = MaterialTheme.colorScheme.shimmer,
        thumbnail = {}
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListItemContainer(
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    maxLines: Int = 1,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    containerColor: Color = Color.Transparent,
    thumbnail: (@Composable (size: Dp) -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    thumbnailHeight: Dp = 56.dp,
    thumbnailAspectRatio: Float = 1F,
    showMoreVert: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            if (isPlaceholder) {
                TextPlaceholder()
            } else {
                Text(
                    text = title,
                    color = titleColor,
                    lineHeight = 16.sp,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                enabled = onClick != null || onLongClick != null,
                onClick = onClick ?: {},
                onLongClick = onLongClick ?: {}
            ),
        supportingContent = {
            if (isPlaceholder) {
                TextPlaceholder()
            } else {
                subtitle?.let {
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        leadingContent = leadingContent ?: thumbnail?.let { thumb ->
            {
                Box(
                    modifier = Modifier
                        .height(height = thumbnailHeight)
                        .aspectRatio(ratio = thumbnailAspectRatio)
                        .clip(MaterialTheme.shapes.medium)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    thumb(thumbnailHeight)
                }
            }
        },
        trailingContent = {
            if (trailingContent != null) {
                trailingContent()
            } else if (showMoreVert && onLongClick != null) {
                IconButton(onClick = onLongClick) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More options"
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = containerColor
        )
    )
}

@Composable
fun ListItemPlaceholder(
    modifier: Modifier = Modifier,
    thumbnailHeight: Dp = 56.dp,
    thumbnailAspectRatio: Float = 1F
) {
    ListItemContainer(
        modifier = modifier,
        isPlaceholder = true,
        title = "",
        color = MaterialTheme.colorScheme.shimmer,
        thumbnail = {},
        thumbnailHeight = thumbnailHeight,
        thumbnailAspectRatio = thumbnailAspectRatio
    )
}

@Composable
fun SongItem(
    modifier: Modifier = Modifier,
    song: Innertube.SongItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    titleColor: Color = Color.Unspecified,
    showThumbnail: Boolean = true,
    thumbnailContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItemContainer(
        modifier = modifier,
        title = song.info?.name ?: "",
        subtitle = song.authors?.mapNotNull { it.name }?.joinToString(" • "),
        titleColor = titleColor,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = leadingContent,
        thumbnail = if (showThumbnail) {
            { size: Dp ->
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
    titleColor: Color = Color.Unspecified,
    showThumbnail: Boolean = true,
    thumbnailContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null,
    showMoreVert: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItemContainer(
        modifier = modifier,
        title = song.title,
        subtitle = listOfNotNull(song.artistsText, song.durationText).joinToString(" • "),
        titleColor = titleColor,
        onClick = onClick,
        onLongClick = onLongClick,
        leadingContent = leadingContent,
        thumbnail = if (showThumbnail) {
            { size: Dp ->
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
        trailingContent = trailingContent,
        showMoreVert = showMoreVert
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSongItem(
    modifier: Modifier = Modifier,
    song: MediaItem,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    titleColor: Color = Color.Unspecified,
    showThumbnail: Boolean = true,
    thumbnailContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItemContainer(
        modifier = modifier,
        title = song.mediaMetadata.title.toString(),
        subtitle = if (song.mediaMetadata.extras?.getString("durationText") == null) {
            song.mediaMetadata.artist.toString()
        } else {
            "${song.mediaMetadata.artist} • ${song.mediaMetadata.extras?.getString("durationText")}"
        },
        titleColor = titleColor,
        onClick = onClick,
        onLongClick = onLongClick,
        containerColor = Color.Transparent,
        leadingContent = leadingContent,
        thumbnail = if (showThumbnail) {
            { size: Dp ->
                Box(contentAlignment = Alignment.Center) {
                    if (thumbnailContent == null) {
                        Icon(
                            painter = painterResource(R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.size(size / 2),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        AsyncImage(
                            model = song.mediaMetadata.artworkUri.thumbnail(size.px),
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
