package com.github.soundpod.enums

import androidx.annotation.StringRes
import com.github.soundpod.R

enum class PlayerLayout(
    @get:StringRes val resourceId: Int
) {
    Default(
        resourceId = R.string.defualt,
    ),
    New(
        resourceId = R.string.new_layout,
    )
}