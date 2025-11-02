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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.soundpod.ui.styling.px
import com.github.soundpod.ui.styling.shimmer
import com.github.soundpod.utils.thumbnail
import com.valentinilk.shimmer.shimmer

fun adaptiveThumbnailContent(
    isLoading: Boolean,
    url: String?
): @Composable () -> Unit = {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val thumbnailSizeDp = maxWidth - 64.dp
        val thumbnailSizePx = thumbnailSizeDp.px

        val modifier = Modifier
            .padding(16.dp)
            .clip(MaterialTheme.shapes.large)
            .size(thumbnailSizeDp)
            .background(MaterialTheme.colorScheme.surfaceVariant)

        if (isLoading) {
            Spacer(
                modifier = modifier
                    .shimmer()
                    .background(MaterialTheme.colorScheme.shimmer)
            )
        } else {
            AsyncImage(
                model = url?.thumbnail(thumbnailSizePx),
                contentDescription = null,
                modifier = modifier
            )
        }
    }
}