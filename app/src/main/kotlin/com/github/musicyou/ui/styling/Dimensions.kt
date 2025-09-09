package com.github.musicyou.ui.styling

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Suppress("ClassName")
object Dimensions {
    val itemsVerticalPadding = 8.dp
    const val mediumOpacity = 0.75F
    const val lowOpacity = 0.5F
    val spacer = 16.dp

    object thumbnails {
        val album = 128.dp
        val artist = 192.dp
        val song = 56.dp
        val playlist = album

        object player {
            val song: Dp
                @Composable
                get() = with(LocalConfiguration.current) {
                    minOf(screenHeightDp, screenWidthDp)
                }.dp
        }
    }
}

inline val Dp.px: Int
    @Composable
    inline get() = with(LocalDensity.current) { roundToPx() }