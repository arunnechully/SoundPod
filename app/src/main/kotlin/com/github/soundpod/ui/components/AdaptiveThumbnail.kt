package com.github.soundpod.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource // <-- Added Import
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.soundpod.R // <-- Added Import (Make sure this matches your package)
import com.github.soundpod.ui.styling.px
import com.github.soundpod.ui.styling.shimmer
import com.github.soundpod.utils.thumbnail
import com.valentinilk.shimmer.shimmer

@Composable
fun AdaptiveThumbnail(
    isLoading: Boolean,
    url: String?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        val baseWidth = if (maxWidth != Dp.Infinity) maxWidth else 200.dp
        val thumbnailSizeDp = (baseWidth - 64.dp).coerceAtLeast(0.dp)

        val thumbnailSizePx = (thumbnailSizeDp.px * 2)

        val sharedModifier = Modifier
            .padding(16.dp)
            .clip(MaterialTheme.shapes.large)
            .size(thumbnailSizeDp)
            .background(MaterialTheme.colorScheme.surfaceVariant)

        if (isLoading) {
            Spacer(
                modifier = sharedModifier
                    .shimmer()
                    .background(MaterialTheme.colorScheme.shimmer)
            )
        } else {
            AsyncImage(
                model = url?.thumbnail(thumbnailSizePx),
                contentDescription = "Track thumbnail",
                contentScale = ContentScale.Crop,
                fallback = painterResource(id = R.drawable.app_icon),
                error = painterResource(id = R.drawable.app_icon),
                modifier = sharedModifier
            )
        }
    }
}