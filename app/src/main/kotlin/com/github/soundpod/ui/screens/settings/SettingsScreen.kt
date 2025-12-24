@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.soundpod.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.github.soundpod.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onOptionClick: (String) -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()

    BackHandler(onBack = onBackClick)

    SettingsScreenLayout(
        title = stringResource(id = R.string.settings),
        onBackClick = onBackClick,
        content = {
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
    )
}