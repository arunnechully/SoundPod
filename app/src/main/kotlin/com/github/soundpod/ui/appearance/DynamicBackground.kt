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
import kotlin.math.max

@Composable
fun DynamicBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    useGradient: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val isDark = isSystemInDarkTheme()

    var targetColor by remember { mutableStateOf(colorPalette.background3) }

    LaunchedEffect(thumbnailUrl, isDark, colorPalette) {
        targetColor = if (thumbnailUrl != null) {
            extractDominantColor(context, thumbnailUrl, colorPalette.background1)
        } else {
            colorPalette.background1
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 800),
        label = "ColorShift"
    )

    val breatheScale by if (animate && useGradient) {
        val infiniteTransition = rememberInfiniteTransition(label = "Breathing")
        infiniteTransition.animateFloat(
            initialValue = 0.8f, targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "Scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val breatheAlpha by if (animate && useGradient) {
        val infiniteTransition = rememberInfiniteTransition(label = "Breathing")
        infiniteTransition.animateFloat(
            initialValue = 0.5f, targetValue = 0.7f,
            animationSpec = infiniteRepeatable(tween(6000), RepeatMode.Reverse), label = "Alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colorPalette.background3)
    ) {
        if (useGradient) {
            // MODE A: Radial Gradient (For Full Player)
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val primaryDimension = max(width, height)
            val currentRadius = (primaryDimension * 0.9f) * breatheScale

            val fusionBrush = Brush.radialGradient(
                colors = listOf(
                    animatedColor.copy(alpha = breatheAlpha),
                    animatedColor.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                center = Offset(x = width / 2, y = height * 1.15f),
                radius = currentRadius
            )

            Box(modifier = Modifier.fillMaxSize().background(fusionBrush))

        } else {
            // MODE B: Solid Color (For Mini Player)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedColor)
                    // --- DIMMING FIX ---
                    // Overlay a semi-transparent black layer to darken the color
                    // Increase alpha (e.g. 0.4f or 0.5f) if you want it even darker.
                    .background(colorPalette.background3.copy(alpha = 0.5f))
            )
        }

        content()
    }
}