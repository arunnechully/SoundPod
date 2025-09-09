package com.github.musicyou.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import com.valentinilk.shimmer.shimmer
import com.github.musicyou.ui.styling.px
import com.github.musicyou.ui.styling.shimmer
import com.github.musicyou.utils.isLandscape
import com.github.musicyou.utils.thumbnail

@Composable
inline fun LayoutWithAdaptiveThumbnail(
    thumbnailContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val isLandscape = isLandscape

    if (isLandscape) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            thumbnailContent()
            content()
        }
    } else {
        content()
    }
}

fun adaptiveThumbnailContent(
    isLoading: Boolean,
    url: String?
): @Composable () -> Unit = {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        //val thumbnailSizeDp = if (isLandscape) (maxHeight - 128.dp) else (maxWidth - 64.dp)
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