@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.soundpod.ui.screens.settings.newsettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.navigation.Routes

@Composable
fun NewSettingsScreen(
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
                    onClick = { navController.navigate(Routes.Appearance) }
                )
                SettingRow(
                    title = stringResource(id = R.string.player),
                    textColor = textColor,
                    icon = Icons.Default.PlayArrow,
                    onClick = { navController.navigate(Routes.Player) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.privacy),
                    textColor = textColor,
                    icon = Icons.Default.PrivacyTip,
                    onClick = { navController.navigate(Routes.Privacy) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.backup_restore),
                    textColor = textColor,
                    icon = Icons.Default.Restore,
                    onClick = { navController.navigate(Routes.Backup) }
                )
                SettingRow(
                    title = stringResource(id = R.string.database),
                    textColor = textColor,
                    icon = Icons.Default.Storage,
                    onClick = { navController.navigate(Routes.Storage) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = stringResource(id = R.string.more_settings),
                    textColor = textColor,
                    painterRes = R.drawable.more_settings,
                    onClick = { navController.navigate(Routes.More) }
                )
                SettingRow(
                    title = stringResource(id = R.string.experimental),
                    textColor = textColor,
                    painterRes = R.drawable.experimental,
                    onClick = { navController.navigate(Routes.Experiment) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingRow(
                    title = "About",
                    textColor = textColor,
                    icon = Icons.Default.Info,
                    onClick = { navController.navigate(Routes.About) }
                )
            }
        }
    )
}

@Composable
fun SettingRow(
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
fun SettingColum(
    textColor: Color,
    icon: IconSource? = null,
    title: String,
    description: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        when (icon) {
            is IconSource.Vector -> Icon(
                imageVector = icon.imageVector,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )

            is IconSource.Icon -> Icon(
                painter = icon.painter,
                contentDescription = title,
                tint = textColor,
                modifier = Modifier.size(28.dp),
            )

            null -> {}
        }

        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }

        trailingContent?.invoke()
    }
}

