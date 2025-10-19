@file:OptIn(ExperimentalMaterial3Api::class)

package com.soundpod.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soundpod.music.R
import com.soundpod.music.ui.components.settings.SettingsCard
import com.soundpod.music.ui.components.settings.SettingsScreenLayout

@Composable
fun SettingsScreen(
    navController: NavController,
    onBackClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    SettingsScreenLayout(
        title = stringResource(id = R.string.settings),
        onBackClick = onBackClick,
        content = {

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.appearance),
                    textColor = textColor,
                    icon = Icons.Default.ColorLens,
                    onClick = { navController.navigate("appearance") }
                )
                SettingRow(
                    title = stringResource(id = R.string.player),
                    textColor = textColor,
                    icon = Icons.Default.PlayArrow,
                    onClick = { navController.navigate("player") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.privacy),
                    textColor = textColor,
                    icon = Icons.Default.PrivacyTip,
                    onClick = { navController.navigate("privacy") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.backup_restore),
                    textColor = textColor,
                    icon = Icons.Default.Restore,
                    onClick = { navController.navigate("backup") }
                )
                SettingRow(
                    title = stringResource(id = R.string.storage),
                    textColor = textColor,
                    icon = Icons.Default.Storage,
                    onClick = { navController.navigate("storage") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.more_settings),
                    textColor = textColor,
                    painterRes = R.drawable.more_settings,
                    onClick = { navController.navigate("more") }
                )
                SettingRow(
                    title = "Experimental",
                    textColor = textColor,
                    painterRes = R.drawable.experimental,
                    onClick = { navController.navigate("experiment") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = "About",
                    textColor = textColor,
                    icon = Icons.Default.Info,
                    onClick = { navController.navigate("about") }
                )
            }
        }
    )
}

@Composable
private fun SettingRow(
    title: String,
    textColor: Color,
    icon: ImageVector? = null,
    painterRes: Int? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
        } else if (painterRes != null) {
            Icon(
                painter = painterResource(id = painterRes),
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }
}


@Composable
private fun SettingColum(
    textColor: Color,
    title: String,
    description: String,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                onClick = {}
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.7f)
        )
    }
}