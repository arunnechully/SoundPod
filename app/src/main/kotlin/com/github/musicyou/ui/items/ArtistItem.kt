package com.github.musicyou.ui.items

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import com.github.innertube.Innertube
import com.github.musicyou.R
import com.github.musicyou.models.Artist
import com.github.musicyou.ui.styling.px
import com.github.musicyou.utils.thumbnail

@Composable
fun ArtistItem(
    modifier: Modifier = Modifier,
    artist: Innertube.ArtistItem,
    onClick: () -> Unit
) {
    ItemContainer(
        modifier = modifier,
        title = artist.info?.name ?: "",
        subtitle = artist.subscribersCountText?.replace(
            oldValue = "subscribers",
            newValue = stringResource(id = R.string.subscribers).lowercase()
        ),
        textAlign = TextAlign.Center,
        shape = CircleShape,
        onClick = onClick
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = artist.thumbnail?.url.thumbnail(maxWidth.px),
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
fun LocalArtistItem(
    modifier: Modifier = Modifier,
    artist: Artist,
    onClick: () -> Unit
) {
    ItemContainer(
        modifier = modifier,
        title = artist.name ?: "",
        textAlign = TextAlign.Center,
        shape = CircleShape,
        onClick = onClick
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = artist.thumbnailUrl.thumbnail(maxWidth.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(MaterialTheme.shapes.large)
            )
        }
    }
}