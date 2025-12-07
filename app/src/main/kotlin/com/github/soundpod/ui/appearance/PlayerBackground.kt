package com.github.soundpod.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.github.soundpod.utils.rememberPreference

const val PLAYER_BACKGROUND_STYLE_KEY = "player_background_style"

// --- NEW CUSTOMIZATION KEYS ---
const val PLAYER_BACKGROUND_CUSTOM_COLOR_1 = "player_bg_color_1" // Primary / Start
const val PLAYER_BACKGROUND_CUSTOM_COLOR_2 = "player_bg_color_2" // Secondary / End (-1 = None)
const val PLAYER_BACKGROUND_IS_ANIMATED = "player_bg_animated"   // True = Breathing, False = Static
const val PLAYER_BACKGROUND_CUSTOM_IMAGE_KEY = "player_background_custom_image"

object BackgroundStyles {
    const val DYNAMIC = 0
    const val ABSTRACT_1 = 1
    const val ABSTRACT_2 = 2
    const val ABSTRACT_3 = 3
    const val ABSTRACT_4 = 4
    const val CUSTOM_IMAGE = 99
}

@Composable
fun PlayerBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val currentStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val customImagePath by rememberPreference(PLAYER_BACKGROUND_CUSTOM_IMAGE_KEY, "")

    // Read new customization prefs
    val isAnimated by rememberPreference(PLAYER_BACKGROUND_IS_ANIMATED, true)

    Box(modifier = modifier.fillMaxSize()) {
        when (currentStyle) {
            BackgroundStyles.DYNAMIC -> {
                // Now passing the animation preference
                DynamicBackground(
                    thumbnailUrl = thumbnailUrl,
                    animate = isAnimated,
                    content = {}
                )
            }
            BackgroundStyles.CUSTOM_IMAGE -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    if (customImagePath.isNotEmpty()) {
                        AsyncImage(
                            model = customImagePath,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().alpha(0.6f)
                        )
                    }
                }
            }
            else -> {
                ThemedLottieBackground(
                    animationNumber = currentStyle,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
        content()
    }
}