package com.github.soundpod.enums

import androidx.annotation.StringRes
import com.github.soundpod.R

enum class QuickPicksSource(
    @StringRes val resourceId: Int
) {
    Default(
        resourceId = R.string.default_source,
    ),
    Custom(
        resourceId = R.string.custom,
    )
}
