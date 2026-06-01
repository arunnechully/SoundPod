@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.navigation.SettingsDestinations
import com.github.soundpod.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    screenId: String,
    onBackClick: () -> Unit,
    onOptionClick: (String) -> Unit
) {
    val title = when (screenId) {
        SettingsDestinations.MAIN -> stringResource(R.string.settings)
        SettingsDestinations.APPEARANCE -> stringResource(R.string.appearance)
        SettingsDestinations.BACKGROUND -> stringResource(R.string.player_background)
        SettingsDestinations.PLAYER -> stringResource(R.string.player)
        SettingsDestinations.PRIVACY -> stringResource(R.string.privacy)
        SettingsDestinations.BACKUP -> stringResource(R.string.backup_restore)
        SettingsDestinations.DATABASE -> stringResource(R.string.database)
        SettingsDestinations.MORE -> stringResource(R.string.more_settings)
        SettingsDestinations.EXPERIMENT -> stringResource(R.string.experimental)
        SettingsDestinations.ABOUT -> stringResource(R.string.about)
        SettingsDestinations.SLEEP_TIMER -> stringResource(R.string.sleep_timer)
        else -> stringResource(R.string.settings)
    }

    SettingsScreenLayout(
        title = title,
        shape = MaterialTheme.shapes.extraSmall,
        onBackClick = onBackClick,
        content = {
            when (screenId) {
                SettingsDestinations.MAIN -> SettingsMainContent(onOptionClick)
                SettingsDestinations.APPEARANCE -> AppearanceSettingsContent(
                    onBackgroundClick = { onOptionClick(SettingsDestinations.BACKGROUND) }
                )
                SettingsDestinations.BACKGROUND -> BackgroundSettingsContent()
                SettingsDestinations.PLAYER -> PlayerSettingsContent(
                    onSleepTimerClick = { onOptionClick(SettingsDestinations.SLEEP_TIMER) }
                )
                SettingsDestinations.SLEEP_TIMER -> SleepTimerSettingsContent()
                SettingsDestinations.PRIVACY -> PrivacySettingsContent()
                SettingsDestinations.BACKUP -> BackupSettingsContent()
                SettingsDestinations.DATABASE -> CacheSettingsContent()
                SettingsDestinations.MORE -> MoreSettingsContent()
                SettingsDestinations.EXPERIMENT -> ExperimentSettingsContent()
                SettingsDestinations.ABOUT -> AboutSettingsContent()
            }
        }
    )
}

@Composable
fun SettingsMainContent(
    onOptionClick: (String) -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()

    Spacer(modifier = Modifier.height(8.dp))

    sections.forEach { section ->
        SettingsCard {
            section.options.forEach { option ->
                if (option.icon != null) {
                    SettingRow(
                        title = stringResource(id = option.title),
                        icon = IconSource.Vector(option.icon),
                        onClick = { onOptionClick(option.screenId) }
                    )
                } else {
                    option.iconRes?.let { iconResId ->
                        SettingRow(
                            title = stringResource(id = option.title),
                            icon = IconSource.Icon(painterResource(iconResId)),
                            onClick = { onOptionClick(option.screenId) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
