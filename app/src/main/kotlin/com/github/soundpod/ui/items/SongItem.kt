package com.github.soundpod.ui.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.MediaItem
import coil3.compose.AsyncImage
import com.github.innertube.Innertube
import com.github.soundpod.models.Song
import com.github.soundpod.ui.styling.px
import com.github.soundpod.utils.thumbnail

@Composable
fun SongItem(
    modifier: Modifier = Modifier,
    song: Innertube.SongItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItemContainer(
        modifier = modifier,
        title = song.info?.name ?: "",
        subtitle = song.authors?.joinToString(separator = "") { it.name ?: "" },
        onClick = onClick,
        onLongClick = onLongClick,
        thumbnail = { size ->
            AsyncImage(
                model = song.thumbnail?.size(size.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
            )
        },
        trailingContent = trailingContent
    )
}

@Composable
fun LocalSongItem(
    modifier: Modifier = Modifier,
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    thumbnailContent: @Composable (() -> Unit)? = null,
    onThumbnailContent: @Composable (BoxScope.() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItemContainer(
        modifier = modifier,
        title = song.title,
        subtitle = "${song.artistsText} • ${song.durationText}",
        onClick = onClick,
        onLongClick = onLongClick,
        thumbnail = { size ->
            Box {
                if (thumbnailContent == null) {
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
        },
        trailingContent = trailingContent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSongItem(
    modifier: Modifier = Modifier,
    song: MediaItem,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onThumbnailContent: @Composable (() -> Unit)? = null,
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
        onClick = onClick,
        onLongClick = onLongClick,
        containerColor = BottomSheetDefaults.ContainerColor,
        thumbnail = { size ->
            Box {
                AsyncImage(
                    model = song.mediaMetadata.artworkUri.thumbnail(size.px),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                )

                onThumbnailContent?.invoke()
            }
        },
        trailingContent = trailingContent
    )
}