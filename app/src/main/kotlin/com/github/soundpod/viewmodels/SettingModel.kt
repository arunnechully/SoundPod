package com.github.soundpod.viewmodels

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class SettingOption(
    @StringRes val title: Int,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
    val screenId: String
)

data class SettingsSection(
    val options: List<SettingOption>
)