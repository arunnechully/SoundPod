package com.github.soundpod.ui.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.github.soundpod.models.Album
import com.github.soundpod.ui.styling.px
import com.github.soundpod.utils.thumbnail

@Composable
fun AlbumItem(
    modifier: Modifier = Modifier,
    album: Innertube.AlbumItem,
    onClick: () -> Unit
) {
    ItemContainer(
        modifier = modifier,
        title = album.info?.name ?: "",
        subtitle = if (album.authors.isNullOrEmpty()) album.year
        else "${album.authors?.joinToString(separator = "") { it.name ?: "" }} • ${album.year}",
        onClick = onClick
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier.size(this@BoxWithConstraints.maxWidth / 2),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            AsyncImage(
                model = album.thumbnail?.url.thumbnail(maxWidth.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(MaterialTheme.shapes.large)
            )
        }
    }
}

@Composable
fun LocalAlbumItem(
    modifier: Modifier = Modifier,
    album: Album,
    onClick: () -> Unit
) {
    ItemContainer(
        modifier = modifier,
        title = album.title ?: "",
        subtitle = if (album.authorsText.isNullOrEmpty()) album.year
        else "${album.authorsText} • ${album.year}",
        onClick = onClick
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier.size(this@BoxWithConstraints.maxWidth / 2),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            AsyncImage(
                model = album.thumbnailUrl?.thumbnail(maxWidth.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(MaterialTheme.shapes.large)
            )
        }
    }
}
