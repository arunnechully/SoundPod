@file:OptIn(ExperimentalMaterial3Api::class)

package com.soundpod.music.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soundpod.music.ui.components.SettingsCard

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val containerColor = if (isDarkTheme) Color.Black else Color(0xFFF6F6F8)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = containerColor,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp),
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = textColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppearanceSettingsCard(textColor)

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Player",
                style = MaterialTheme.typography.titleMedium,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            PlayerSettingsCard(textColor)

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Advanced",
                style = MaterialTheme.typography.titleMedium,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            AdvancedSettingsCard(textColor)
        }
    }
}

@Composable
private fun AppearanceSettingsCard(textColor: Color) {
    var darkModeEnabled by remember { mutableStateOf(false) }
    var dynamicColorEnabled by remember { mutableStateOf(true) }

    SettingsCard {
        SettingRow(
            title = "Dark Mode",
            description = "Use a dark theme across the app",
            textColor = textColor,
            switchState = darkModeEnabled,
            onSwitchChange = { darkModeEnabled = it }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SettingRow(
            title = "Dynamic Color",
            description = "Match system accent color",
            textColor = textColor,
            switchState = dynamicColorEnabled,
            onSwitchChange = { dynamicColorEnabled = it }
        )
    }
}

@Composable
private fun PlayerSettingsCard(textColor: Color) {
    var crossfadeEnabled by remember { mutableStateOf(true) }
    var autoplayEnabled by remember { mutableStateOf(false) }

    SettingsCard {
        SettingRow(
            title = "Crossfade",
            description = "Smoothly transition between tracks",
            textColor = textColor,
            switchState = crossfadeEnabled,
            onSwitchChange = { crossfadeEnabled = it }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        SettingRow(
            title = "Autoplay",
            description = "Automatically play next recommended song",
            textColor = textColor,
            switchState = autoplayEnabled,
            onSwitchChange = { autoplayEnabled = it }
        )
    }
}

@Composable
private fun AdvancedSettingsCard(textColor: Color) {
    var analyticsEnabled by remember { mutableStateOf(true) }

    SettingsCard {
        SettingRow(
            title = "Pause Playback history",
            description = "Disables Recommendations",
            textColor = textColor,
            switchState = analyticsEnabled,
            onSwitchChange = { analyticsEnabled = it }
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Clear Cache",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Text(
                text = "Free up space by clearing temporary files",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    textColor: Color,
    switchState: Boolean,
    onSwitchChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = textColor)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = switchState,
            onCheckedChange = onSwitchChange
        )
    }
}
