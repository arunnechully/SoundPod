@file:Suppress("NOTHING_TO_INLINE")

package com.github.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

@JvmInline
value class Px(val value: Int) {
    inline val dp @Composable get() = dp(LocalDensity.current)
    inline fun dp(density: Density) = with(density) { value.toDp() }
}

inline val Int.px inline get() = Px(value = this)
inline val Float.px inline get() = roundToInt().px

inline val Dp.px: Int
    @Composable
    inline get() = with(LocalDensity.current) { roundToPx() }

inline val TextUnit.dp
    @Composable
    inline get() = dp(LocalDensity.current)

inline fun TextUnit.dp(density: Density) = with(density) { toDp() }
