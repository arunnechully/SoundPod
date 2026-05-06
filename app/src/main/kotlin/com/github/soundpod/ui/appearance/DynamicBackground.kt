package com.github.soundpod.ui.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.github.core.ui.LocalAppearance
import com.github.soundpod.utils.rememberPreference
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

    val prefColor1 by rememberPreference(PLAYER_BACKGROUND_CUSTOM_COLOR_1, -1)
    val prefColor2 by rememberPreference(PLAYER_BACKGROUND_CUSTOM_COLOR_2, -1)
    val userWantsAnimation by rememberPreference(PLAYER_BACKGROUND_IS_ANIMATED, true)

    val shouldAnimate = animate && userWantsAnimation

    // 1. Color Logic: Use derivedStateOf to avoid unnecessary calculations
    var primaryColor by remember { mutableStateOf(colorPalette.background3) }
    var secondaryColor by remember { mutableStateOf(Color.Transparent) }

    LaunchedEffect(thumbnailUrl, prefColor1, prefColor2, colorPalette) {
        primaryColor = when {
            prefColor1 != -1 -> Color(prefColor1)
            thumbnailUrl != null -> extractDominantColor(context, thumbnailUrl, colorPalette.background1)
            else -> colorPalette.background1
        }
        secondaryColor = if (prefColor2 != -1) Color(prefColor2) else Color.Transparent
    }

    val animatedPrimary by animateColorAsState(primaryColor, tween(800), label = "Primary")
    val animatedSecondary by animateColorAsState(secondaryColor, tween(800), label = "Secondary")

    // 2. Animation Logic: Move infiniteTransition outside the draw loop
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundBreath")

    val scaleAnim = if (shouldAnimate) {
        infiniteTransition.animateFloat(0.8f, 1.1f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), "scale")
    } else remember { mutableFloatStateOf(1.0f) }

    val alphaAnim = if (shouldAnimate) {
        infiniteTransition.animateFloat(0.5f, 0.7f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), "alpha")
    } else remember { mutableFloatStateOf(0.6f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorPalette.background3) // Static base color
    ) {
        if (useGradient) {
            // 3. Optimization: Use drawWithCache to avoid recreating Brushes on every frame
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val width = size.width
                        val height = size.height
                        val primaryDim = max(width, height)

                        val brush = if (prefColor2 != -1) {
                            Brush.verticalGradient(listOf(animatedPrimary, animatedSecondary))
                        } else {
                            Brush.radialGradient(
                                colors = listOf(
                                    animatedPrimary.copy(alpha = alphaAnim.value),
                                    animatedPrimary.copy(alpha = 0.2f),
                                    Color.Transparent
                                ),
                                center = Offset(width / 2f, height * 1.15f),
                                radius = primaryDim * 0.9f * scaleAnim.value
                            )
                        }
                        onDrawBehind {
                            drawRect(brush)
                        }
                    }
            )
        } else {
            // Mini Player optimized (Solid background)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(animatedPrimary)
                    .background(colorPalette.background3.copy(0.5f))
            )
        }
        content()
    }
}