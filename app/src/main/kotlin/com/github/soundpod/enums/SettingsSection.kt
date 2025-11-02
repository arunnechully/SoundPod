package com.github.soundpod.enums

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.More
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.soundpod.R

enum class SettingsSection(
    @StringRes val resourceId: Int,
    val icon: ImageVector
) {
    General(
        resourceId = R.string.general,
        Icons.Outlined.Tune
    ),
    Player(
        resourceId = R.string.player,
        icon = Icons.Outlined.PlayArrow
    ),
    Gestures(
        resourceId = R.string.gestures,
        icon = Icons.Outlined.Gesture
    ),
    Cache(
        resourceId = R.string.cache,
        icon = Icons.Outlined.History
    ),
    Database(
        resourceId = R.string.database,
        icon = Icons.Outlined.Save
    ),
    Other(
        resourceId = R.string.other,
        icon = Icons.AutoMirrored.Outlined.More
    ),
    About(
        resourceId = R.string.about,
        icon = Icons.Outlined.Info
    )
}