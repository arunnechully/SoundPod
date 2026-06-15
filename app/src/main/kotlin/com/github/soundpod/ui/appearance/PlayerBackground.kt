package com.github.soundpod.ui.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import com.github.core.ui.LocalAppearance
import com.github.core.visuals.ColorClusters
import com.github.core.visuals.BlurredBackground
import com.github.core.visuals.ThemedLottieBackground
import com.github.core.visuals.adaptToTheme
import com.github.core.visuals.extractColorClusters
import com.github.soundpod.utils.rememberPreference

const val PLAYER_BACKGROUND_STYLE_KEY = "player_background_style"

object BackgroundStyles {
    const val OFF = -1
    const val STATIC = 0
    const val ABSTRACT_1 = 1
    const val ABSTRACT_2 = 2
    const val ABSTRACT_3 = 3
    const val ABSTRACT_4 = 4
    const val BLURRED = 5
}

@Composable
fun PlayerBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    expandProgress: Float = 0f,
    content: @Composable () -> Unit
) {
    val currentStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.BLURRED)
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current

    val fallbackColor = colorPalette.accent

    var clusters by remember { 
        mutableStateOf(ColorClusters(fallbackColor, fallbackColor, fallbackColor, fallbackColor, fallbackColor, fallbackColor))
    }
    
    LaunchedEffect(thumbnailUrl, colorPalette) {
        clusters = extractColorClusters(context, thumbnailUrl, fallbackColor)
    }

    val isDark = colorPalette.isDark
    val baseBackground = if (isDark) Color(0xFF05050A) else Color(0xFFFAFAFF)
    val targetBackgroundColorRaw = remember(clusters, isDark) {
        val adapted = clusters.surface.adaptToTheme(isDark)
        adapted.copy(alpha = if (isDark) 0.35f else 0.25f).compositeOver(baseBackground)
    }
    val targetBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColorRaw,
        animationSpec = tween(1200),
        label = "targetBackgroundColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(targetBackgroundColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().alpha(expandProgress)) {
            when (currentStyle) {
                BackgroundStyles.OFF -> {
                    Box(Modifier.fillMaxSize().background(baseBackground))
                }
                BackgroundStyles.STATIC -> {
                    val staticColor by animateColorAsState(
                        targetValue = clusters.c1,
                        animationSpec = tween(1000),
                        label = "staticColor"
                    )
                    Box(Modifier.fillMaxSize().background(baseBackground))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to staticColor.copy(alpha = if (isDark) 0.45f else 0.35f),
                                    0.5f to staticColor.copy(alpha = if (isDark) 0.45f else 0.35f),
                                    1f to Color.Transparent
                                )
                            )
                    )
                }
                BackgroundStyles.BLURRED -> {
                    BlurredBackground(
                        thumbnailUrl = thumbnailUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    ThemedLottieBackground(
                        animationNumber = currentStyle,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        content()
    }
}
