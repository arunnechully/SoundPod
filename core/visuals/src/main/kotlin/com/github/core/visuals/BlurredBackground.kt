package com.github.core.visuals

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun BlurredBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = thumbnailUrl,
        transitionSpec = {
            fadeIn(animationSpec = tween(1200)) togetherWith
            fadeOut(animationSpec = tween(1200))
        },
        label = "BlurredBackgroundTransition",
        modifier = modifier.fillMaxSize()
    ) { targetUrl ->
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(targetUrl)
                .size(64)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    renderEffect = null // This helps avoid some edge cases on older versions
                    clip = true
                }
                .blur(70.dp)
                .alpha(0.45f),
            contentScale = ContentScale.Crop
        )
    }
}
