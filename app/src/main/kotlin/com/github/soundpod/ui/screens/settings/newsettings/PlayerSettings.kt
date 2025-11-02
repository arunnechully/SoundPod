package com.github.soundpod.ui.screens.settings.newsettings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.soundpod.R
import com.github.soundpod.ui.components.CustomStyledSlider
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlayerSettings(
    onBackClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    var skipSilenceEnabled by remember { mutableStateOf(false) }
    var autoPauseEnabled by remember { mutableStateOf(false) }
    var playSpeed by remember { mutableFloatStateOf(1.0f) }
    var crossfade by remember { mutableFloatStateOf(5f) }
    var normalizeEnabled by remember { mutableStateOf(false) }
    var autoResume by remember { mutableStateOf(false) }

    SettingsScreenLayout(
        title = stringResource(id = R.string.player),
        onBackClick = onBackClick,
        content = {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingColum(
                    textColor = textColor,
                    icon = Icons.Default.Timer,
                    title = "Sleep Timer",
                    description = "Set playback duration",
                    onClick = {}
                )
                SwitchSetting(
                    textColor = textColor,
                    icon = Icons.Default.MusicOff,
                    title = "Skip Silence",
                    description = "Skip silent parts during playback",
                    switchState = skipSilenceEnabled,
                    onSwitchChange = { skipSilenceEnabled = it }
                )
                SwitchSetting(
                    textColor = textColor,
                    painterRes = R.drawable.audio_focus,
                    title = "Audio Focus",
                    description = "Pause playback when other media is playing",
                    switchState = true,
                    onSwitchChange = {}
                )
                SwitchSetting(
                    textColor = textColor,
                    icon = Icons.Default.Pause,
                    title = "Auto Pause",
                    description = "Pause playback when volume is muted",
                    switchState = autoPauseEnabled,
                    onSwitchChange = { autoPauseEnabled = it }
                )
                SwitchSetting(
                    textColor = textColor,
                    painterRes = R.drawable.headphone,
                    title = "Auto Resume",
                    description = "Play automatically when headphones connect",
                    switchState = autoResume,
                    onSwitchChange = { autoResume = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Audio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                CustomStyledSlider(
                    label = "Play speed",
                    value = playSpeed,
                    onValueChange = { playSpeed = it },
                    valueRange = 0.5f..2.0f,
                    valueLabel = {
                        String.format(Locale.US, "%.1fx", it)
                    }
                )

                CustomStyledSlider(
                    label = "Crossfade between tracks",
                    value = crossfade,
                    onValueChange = { crossfade = it },
                    valueRange = 0f..10f,
                    valueLabel = {
                        val seconds = it.toInt()
                        if (seconds == 0) "Off" else "$seconds seconds"
                    }
                )

                SwitchSetting(
                    textColor = textColor,
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Normalize Volume",
                    description = "Keep consistent loudness across songs",
                    switchState = normalizeEnabled,
                    onSwitchChange = { normalizeEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    )
}