package com.github.soundpod.enums

import androidx.annotation.StringRes
import com.github.soundpod.R

enum class ProgressBar(
    @get:StringRes val resourceId: Int
) {
    Default(
        resourceId = R.string.defualt,
    ),
    Animated(
        resourceId = R.string.animated,
    )
}