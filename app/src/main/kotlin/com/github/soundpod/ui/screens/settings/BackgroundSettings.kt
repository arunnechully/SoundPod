package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.ui.appearance.BackgroundStyles
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_STYLE_KEY
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.rememberPreference
import androidx.core.content.edit

@Composable
fun BackgroundSettings(
    onBackClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current

    // Read current state
    val currentStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)

    SettingsScreenLayout(
        title = "Player Background",
        onBackClick = onBackClick
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Styles",
            style = MaterialTheme.typography.titleMedium,
            color = colorPalette.text,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsCard {
            // Option 1: Dynamic
            BackgroundOptionItem(
                title = "Dynamic Fusion",
                description = "Adapts to album art colors",
                selected = currentStyle == BackgroundStyles.DYNAMIC,
                onClick = {
                    // FIX: Use standard SharedPreferences edit
                    context.preferences.edit {
                        putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
                    }
                }
            )

            // Option 2: Abstract 1
            BackgroundOptionItem(
                title = "Abstract Waves",
                description = "Animated looped background",
                selected = currentStyle == BackgroundStyles.ABSTRACT_1,
                onClick = {
                    context.preferences.edit {
                        putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.ABSTRACT_1)
                    }
                }
            )

            // Option 3: Abstract 2
            BackgroundOptionItem(
                title = "Abstract Particles",
                description = "Animated looped background",
                selected = currentStyle == BackgroundStyles.ABSTRACT_2,
                onClick = {
                    context.preferences.edit {
                        putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.ABSTRACT_2)
                    }
                }
            )

            // Option 4: Abstract 3
            BackgroundOptionItem(
                title = "Abstract Gradient",
                description = "Animated looped background",
                selected = currentStyle == BackgroundStyles.ABSTRACT_3,
                onClick = {
                    context.preferences.edit {
                        putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.ABSTRACT_3)
                    }
                }
            )

            BackgroundOptionItem(
                title = "Abstract Gradient",
                description = "Animated looped background",
                selected = currentStyle == BackgroundStyles.ABSTRACT_4,
                onClick = {
                    context.preferences.edit {
                        putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.ABSTRACT_4)
                    }
                }
            )
        }
    }
}

@Composable
fun BackgroundOptionItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colorPalette.text)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = colorPalette.textSecondary)
        }

        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = colorPalette.accent)
        }
    }
}