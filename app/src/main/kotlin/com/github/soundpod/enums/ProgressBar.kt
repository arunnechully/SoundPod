package com.github.soundpod.enums

import androidx.annotation.StringRes
import com.github.soundpod.R

enum class ProgressBar(
    @get:StringRes val resourceId: Int
) {
    Default(
        resourceId = R.string.defualt,
    ),
    Wave(
        resourceId = R.string.wave,
    ),
    Paperboat(
    resourceId = R.string.paperboat,
    )
}