package com.github.soundpod.ui.appearance

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import com.github.core.ui.LocalAppearance
import com.github.soundpod.utils.rememberPreference

const val PLAYER_BACKGROUND_STYLE_KEY = "player_background_style"

object BackgroundStyles {
    const val ABSTRACT_1 = 1
    const val ABSTRACT_2 = 2
    const val ABSTRACT_3 = 3
    const val ABSTRACT_4 = 4
    const val MORPHING = 5
}

@Composable
fun PlayerBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    expandProgress: Float = 0f,
    content: @Composable () -> Unit
) {
    val currentStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.MORPHING)
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current

    val fallbackColor = colorPalette.accent

    var clusters by remember { 
        mutableStateOf(ColorClusters(fallbackColor, fallbackColor, fallbackColor, fallbackColor)) 
    }
    
    LaunchedEffect(thumbnailUrl, colorPalette) {
        clusters = extractColorClusters(context, thumbnailUrl, fallbackColor)
    }

    val isDark = colorPalette.isDark
    val baseBackground = if (isDark) Color(0xFF05050A) else Color(0xFFFAFAFF)

    // Improved solid fill color logic
    val miniPlayerBackgroundColor = remember(clusters, isDark) {
        // Adapt the surface color to ensure it's not too dark in dark mode or too light in light mode
        val adapted = clusters.surface.adaptToTheme(isDark)
        
        // Use a consistent alpha that works well with the base background
        adapted.copy(alpha = if (isDark) 0.35f else 0.25f).compositeOver(baseBackground)
    }

    Box(modifier = modifier.fillMaxSize().background(miniPlayerBackgroundColor)) {
        // Overlay the animated background layer as the player expands
        Box(modifier = Modifier.fillMaxSize().alpha(expandProgress)) {
            when (currentStyle) {
                BackgroundStyles.MORPHING, 0 -> {
                    MorphingBackground(
                        colors = clusters,
                        isDark = isDark,
                        isPlaying = isPlaying
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
