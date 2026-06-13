package com.github.soundpod.ui.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale
import coil3.compose.AsyncImage
import com.github.innertube.Innertube
import com.github.soundpod.R
import com.github.soundpod.models.Artist
import com.github.soundpod.ui.styling.px
import com.github.soundpod.utils.thumbnail

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
            newValue = stringResource(id = R.string.subscribers).lowercase(Locale.ROOT)
        ),
        textAlign = TextAlign.Center,
        shape = CircleShape,
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
                model = artist.thumbnailUrl.thumbnail(maxWidth.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.clip(MaterialTheme.shapes.large)
            )
        }
    }
}
