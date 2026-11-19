package com.github.soundpod.enums

import androidx.annotation.StringRes
import com.github.soundpod.R

enum class PlayerLayout(
    @get:StringRes val resourceId: Int
) {
    SamsungMusic(
        resourceId = R.string.samsung_music,
    ),
    New(
        resourceId = R.string.new_layout,
    ),
    AppleMusic(
        resourceId = R.string.apple_music,
    )
}