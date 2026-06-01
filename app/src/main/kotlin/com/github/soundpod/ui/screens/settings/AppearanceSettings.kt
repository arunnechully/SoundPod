package com.github.soundpod.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.soundpod.R
import com.github.soundpod.enums.AppThemeColor
import com.github.soundpod.enums.PlayerLayout
import com.github.soundpod.enums.ProgressBar
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.utils.appTheme
import com.github.soundpod.utils.playerlayout
import com.github.soundpod.utils.progressBarStyle
import com.github.soundpod.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    onBackClick: () -> Unit,
    onBackgroundClick: () -> Unit
) {
    var appThemeColor by rememberPreference(appTheme, AppThemeColor.System)
    var progressBarStyle by rememberPreference(progressBarStyle, ProgressBar.Default )
    var playerlayout by rememberPreference(playerlayout, PlayerLayout.Default )

    BackHandler(onBack = onBackClick)

    SettingsScreenLayout(
        title = stringResource(id = R.string.appearance),
        shape = MaterialTheme.shapes.extraSmall,
        onBackClick = onBackClick,
        content = {
            SettingsGroup(
                title = stringResource(id = R.string.theme),
            ) {
                EnumValueSelectorSettingsEntry(
                    title = stringResource(id = R.string.app_theme),
                    selectedValue = appThemeColor,
                    onValueSelected = { appThemeColor = it },
                    icon = IconSource.Icon( painterResource(id = R.drawable.dark_mode)),
                    valueText = { stringResource(it.resourceId) }
                )
            }

            SettingsGroup(
                title = stringResource(id = R.string.player_style),
            ) {
                EnumValueSelectorSettingsEntry(
                    title = stringResource(id = R.string.player_layout),
                    selectedValue = playerlayout,
                    onValueSelected = { playerlayout = it },
                    icon = IconSource.Icon( painterResource(id = R.drawable.layout)),
                    valueText = { stringResource(it.resourceId) }
                )
            }

            SettingsGroup{
                EnumValueSelectorSettingsEntry(
                    title = stringResource(id = R.string.progress_bar_style),
                    selectedValue = progressBarStyle,
                    onValueSelected = { progressBarStyle = it },
                    icon = IconSource.Icon( painterResource(id = R.drawable.wave)),
                    valueText = { stringResource(it.resourceId) }
                )
                SettingsColumn(
                    icon = IconSource.Vector(Icons.Default.BlurOn),
                    title = stringResource(id = R.string.background_style),
                    description = stringResource(id = R.string.background_style_discription),
                    onClick = onBackgroundClick
                )
            }
        }
    )
}