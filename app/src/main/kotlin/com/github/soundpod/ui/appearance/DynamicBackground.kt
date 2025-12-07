package com.github.soundpod.ui.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    val isDark = isSystemInDarkTheme()


    val prefColor1 by rememberPreference(PLAYER_BACKGROUND_CUSTOM_COLOR_1, -1)
    val prefColor2 by rememberPreference(PLAYER_BACKGROUND_CUSTOM_COLOR_2, -1)

    // Logic: If 'animate' param is true, we still respect the user's "Disable Animation" toggle.
    // If 'animate' param is false (MiniPlayer), we force it off.
    val userWantsAnimation by rememberPreference(PLAYER_BACKGROUND_IS_ANIMATED, true)
    val shouldAnimate = animate && userWantsAnimation

    // --- COLOR CALCULATION ---
    var primaryColor by remember { mutableStateOf(colorPalette.background3) }
    var secondaryColor by remember { mutableStateOf(Color.Transparent) }

    LaunchedEffect(thumbnailUrl, prefColor1, prefColor2, isDark, colorPalette) {
        // 1. Determine Primary Color
        primaryColor = if (prefColor1 != -1) {
            Color(prefColor1) // Custom Solid
        } else {
            // Auto Extract
            if (thumbnailUrl != null) extractDominantColor(context, thumbnailUrl, colorPalette.background1)
            else colorPalette.background1
        }

        // 2. Determine Secondary Color (For Gradient)
        secondaryColor = if (prefColor2 != -1) {
            Color(prefColor2) // Custom Gradient End
        } else {
            Color.Transparent // Default Fusion style (fades to nothing)
        }
    }

    val animatedPrimary by animateColorAsState(primaryColor, tween(800), label = "Primary")
    val animatedSecondary by animateColorAsState(secondaryColor, tween(800), label = "Secondary")

    // --- ANIMATION VALUES ---
    // If static, we lock scale/alpha to specific values
    val scale by if (shouldAnimate) {
        rememberInfiniteTransition("breath").animateFloat(0.8f, 1.1f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), "s")
    } else remember { mutableFloatStateOf(1.0f) }

    val alpha by if (shouldAnimate) {
        rememberInfiniteTransition("breath").animateFloat(0.5f, 0.7f, infiniteRepeatable(tween(6000), RepeatMode.Reverse), "a")
    } else remember { mutableFloatStateOf(0.6f) } // Slightly higher opacity for static

    // --- RENDER ---
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colorPalette.background3)) {
        if (useGradient) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val primaryDim = max(width, height)

            // If Secondary Color is SET (Not Transparent/None), we do a Linear Gradient
            // If Secondary Color is NONE, we do the Radial Fusion (Samsung Style)

            val finalBrush = if (prefColor2 != -1) {
                // LINEAR GRADIENT (User picked 2 colors)
                Brush.verticalGradient(
                    colors = listOf(animatedPrimary, animatedSecondary)
                )
            } else {
                // RADIAL FUSION (User picked 1 color or Auto)
                Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = alpha),
                        animatedPrimary.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(width / 2, height * 1.15f),
                    radius = primaryDim * 0.9f * scale
                )
            }

            Box(Modifier.fillMaxSize().background(finalBrush))

        } else {
            // Mini Player (Solid)
            Box(Modifier.fillMaxSize().background(animatedPrimary).background(colorPalette.background3.copy(0.5f)))
        }
        content()
    }
}