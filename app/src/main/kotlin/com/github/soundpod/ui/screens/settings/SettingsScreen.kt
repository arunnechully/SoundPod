@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.soundpod.R
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.navigation.Routes

@Composable
fun SettingsScreen(
    navController: NavController,
    onBackClick: () -> Unit
) {
    SettingsScreenLayout(
        title = stringResource(id = R.string.settings),
        onBackClick = onBackClick,
        content = {

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.appearance),
                    icon = Icons.Default.ColorLens,
                    onClick = { navController.navigate(Routes.Appearance) }
                )
                SettingRow(
                    title = stringResource(id = R.string.player),
                    icon = Icons.Default.PlayArrow,
                    onClick = { navController.navigate(Routes.Player) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.privacy),
                    icon = Icons.Default.PrivacyTip,
                    onClick = { navController.navigate(Routes.Privacy) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.backup_restore),
                    icon = Icons.Default.Restore,
                    onClick = { navController.navigate(Routes.Backup) }
                )
                SettingRow(
                    title = stringResource(id = R.string.database),
                    icon = Icons.Default.Storage,
                    onClick = { navController.navigate(Routes.Storage) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.more_settings),
                    painterRes = R.drawable.more_settings,
                    onClick = { navController.navigate(Routes.More) }
                )
                SettingRow(
                    title = stringResource(id = R.string.experimental),
                    painterRes = R.drawable.experimental,
                    onClick = { navController.navigate(Routes.Experiment) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = "About",
                    icon = Icons.Default.Info,
                    onClick = { navController.navigate(Routes.About) }
                )
            }
        }
    )
}
