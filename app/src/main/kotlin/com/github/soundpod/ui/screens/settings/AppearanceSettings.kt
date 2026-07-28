package com.github.soundpod.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
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
import com.github.soundpod.utils.HomeTab
import com.github.soundpod.utils.TabStyle
import com.github.soundpod.utils.appearanceUpdatedKey
import com.github.soundpod.utils.appTheme
import com.github.soundpod.utils.playerlayout
import com.github.soundpod.utils.progressBarStyle
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.tabStyleKey
import androidx.compose.runtime.LaunchedEffect

@Composable
fun AppearanceSettingsContent(
    onBackgroundClick: () -> Unit
) {
    var appThemeColor by rememberPreference(appTheme, AppThemeColor.System)
    var progressBarStyle by rememberPreference(progressBarStyle, ProgressBar.Default )
    var playerlayout by rememberPreference(playerlayout, PlayerLayout.Default )
    var tabStyle by rememberPreference(tabStyleKey, TabStyle.Modern)

    var isAppearanceUpdated by rememberPreference(appearanceUpdatedKey, false)

    LaunchedEffect(Unit) {
        isAppearanceUpdated = false
    }

    val homeTabsVisibility = HomeTab.entries.associateWith { tab ->
        rememberPreference(tab.key, true)
    }

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
            onClick = onBackgroundClick,
        )
    }

    SettingsGroup(
        title = stringResource(id = R.string.home_screen_tabs),
    ) {
        EnumValueSelectorSettingsEntry(
            title = stringResource(id = R.string.tab_style),
            selectedValue = tabStyle,
            onValueSelected = { tabStyle = it },
            icon = IconSource.Icon(painterResource(id = R.drawable.interface_ui_tabs)),
            valueText = { stringResource(it.resourceId) }
        )
        MultiSelectorSettingsEntry(
            title = stringResource(id = R.string.home_screen_tabs),
            selectedValues = homeTabsVisibility.filter { it.value.value }.keys.toSet(),
            values = HomeTab.entries,
            onConfirm = { newSelected ->
                HomeTab.entries.forEach { tab ->
                    homeTabsVisibility[tab]?.value = newSelected.contains(tab)
                }
            },
            icon = IconSource.Icon(painterResource(id = R.drawable.tabs)),
            valueText = { stringResource(id = it.title) }
        )
    }

}
