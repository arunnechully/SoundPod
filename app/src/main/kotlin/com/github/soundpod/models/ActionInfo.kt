package com.github.soundpod.models

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class ActionInfo(
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val icon: ImageVector,
    @StringRes val description: Int
)