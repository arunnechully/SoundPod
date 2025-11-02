package com.github.soundpod.enums

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.soundpod.R
import com.github.soundpod.models.SortBy

enum class SongSortBy(
    @StringRes override val text: Int,
    override val icon: ImageVector
) : SortBy {
    PlayTime(
        text = R.string.play_time,
        icon = Icons.AutoMirrored.Outlined.TrendingUp
    ),
    Title(
        text = R.string.title,
        icon = Icons.Outlined.SortByAlpha
    ),
    DateAdded(
        text = R.string.date_added,
        icon = Icons.Outlined.Schedule
    ),
    Artist(
        text = R.string.artist,
        icon = Icons.Outlined.Person
    )
}