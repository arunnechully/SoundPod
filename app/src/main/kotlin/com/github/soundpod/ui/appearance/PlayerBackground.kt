package com.github.soundpod.ui.appearance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.soundpod.utils.rememberPreference

// 1. Just use a String Key (SharedPreferences doesn't need intPreferencesKey)
const val PLAYER_BACKGROUND_STYLE_KEY = "player_background_style"

object BackgroundStyles {
    const val DYNAMIC = 0
    const val ABSTRACT_1 = 1
    const val ABSTRACT_2 = 2
    const val ABSTRACT_3 = 3
    const val ABSTRACT_4 = 4
}

@Composable
fun PlayerBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // This reads from SharedPreferences using the String key
    val currentStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)

    if (currentStyle == BackgroundStyles.DYNAMIC) {
        DynamicBackground(
            thumbnailUrl = thumbnailUrl,
            modifier = modifier,
            content = content
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            ThemedLottieBackground(
                animationNumber = currentStyle,
                modifier = Modifier.matchParentSize()
            )
            content()
        }
    }
}