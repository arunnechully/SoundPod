package com.github.soundpod.ui.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.github.core.ui.LocalAppearance

@Composable
fun DynamicBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val isDark = isSystemInDarkTheme()

    // Initialize with current theme background
    var targetColor by remember { mutableStateOf(colorPalette.background3) }

    LaunchedEffect(thumbnailUrl, isDark, colorPalette) {
        targetColor = if (thumbnailUrl != null) {
            // FIX: Pass the CURRENT theme background as the default fallback
            extractDominantColor(context, thumbnailUrl, colorPalette.background0)
        } else {
            colorPalette.background1
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 800),
        label = "ColorShift"
    )

    // ... (Rest of the animation code is the same) ...
    val infiniteTransition = rememberInfiniteTransition(label = "Breathing")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "Scale"
    )
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "Alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colorPalette.background3) // Updates immediately on theme switch
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val currentRadius = (height * 0.9f) * breatheScale

        val fusionBrush = Brush.radialGradient(
            colors = listOf(
                animatedColor.copy(alpha = breatheAlpha),
                animatedColor.copy(alpha = 0.2f),
                Color.Transparent
            ),
            center = Offset(x = width / 2, y = height * 1.15f),
            radius = currentRadius
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fusionBrush)
        )
        content()
    }
}