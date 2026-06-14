package com.github.soundpod.enums

import androidx.annotation.StringRes
import com.github.soundpod.R

enum class QuickPicksSource(
    @StringRes val resourceId: Int
) {
    Trending(
        resourceId = R.string.trending,
    ),
    LastPlayed(
        resourceId = R.string.last_played,
    ),
    Recommended(
        resourceId = R.string.recommended,
    ),
    Custom(
        resourceId = R.string.custom,
    )
}
