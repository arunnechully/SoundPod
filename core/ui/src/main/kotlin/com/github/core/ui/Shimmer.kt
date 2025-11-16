package com.github.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.valentinilk.shimmer.defaultShimmerTheme

@Composable
fun shimmerTheme() = remember {
    defaultShimmerTheme.copy(
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = LinearEasing,
                delayMillis = 250
            ),
            repeatMode = RepeatMode.Restart
        ),
        shaderColors = listOf(
            Color.Unspecified.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.50f),
            Color.Unspecified.copy(alpha = 0.25f)
        )
    )
}
