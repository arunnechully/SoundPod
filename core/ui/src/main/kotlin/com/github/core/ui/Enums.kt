package com.github.core.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.core.ui.utils.roundedShape

enum class ThumbnailRoundness(val dp: Dp) {
    None(0.dp),
    Light(2.dp),
    Medium(8.dp),
    Heavy(12.dp),
    Heavier(16.dp),
    Heaviest(18.dp);

    val shape get() = dp.roundedShape
}

enum class ColorSource {
    Default,
    Dynamic,
    MaterialYou,
}

enum class ColorMode {
    System,
    Light,
    Dark
}

enum class Darkness {
    Normal,
    AMOLED,
    PureBlack
}
