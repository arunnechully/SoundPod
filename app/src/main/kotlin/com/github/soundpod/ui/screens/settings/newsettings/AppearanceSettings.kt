package com.github.soundpod.ui.screens.settings.newsettings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.soundpod.R
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Appearance(
    onBackClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    SettingsScreenLayout(
        title =stringResource(id = R.string.appearance),
        onBackClick = onBackClick,
        content = {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = textColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard{
                SettingColum(
                    textColor = textColor,
                    painterRes = R.drawable.dark_mode,
                    title = "App Theme",
                    description = "Select App Theme",
                    onClick = {}
                )
                SettingColum(
                    textColor = textColor,
                    painterRes = R.drawable.color_mode,
                    title = "Accent Color",
                    description = "Choose your preferred accent color",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Animations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = textColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard{
                SettingColum(
                    textColor = textColor,
                    painterRes = R.drawable.wave,
                    title = "Progress Bar Style",
                    description = "Choose your preferred progress bar style",
                    onClick = {}
                )
                SettingColum(
                    textColor = textColor,
                    icon = Icons.Default.BlurOn,
                    title = "Background Style",
                    description = "Choose your preferred background style",
                    onClick = {}
                )
            }
        }
    )
}