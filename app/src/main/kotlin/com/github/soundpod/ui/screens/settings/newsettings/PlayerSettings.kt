package com.github.soundpod.ui.screens.settings.newsettings

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SliderSettingsItem
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.screens.player.SleepTimer
import com.github.soundpod.utils.isAtLeastAndroid6
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.resumePlaybackWhenDeviceConnectedKey
import com.github.soundpod.utils.skipSilenceKey
import com.github.soundpod.utils.volumeNormalizationKey
import kotlinx.coroutines.flow.flowOf
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPlayerSettings(
    onBackClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    var skipSilence by rememberPreference(skipSilenceKey, false)
    var autoPauseEnabled by remember { mutableStateOf(false) }
    var audioFocus by remember { mutableStateOf(false) }
    var playSpeed by remember { mutableFloatStateOf(1.0f) }
    var crossfade by remember { mutableFloatStateOf(5f) }
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)
    var resumePlaybackWhenDeviceConnected by rememberPreference(
        resumePlaybackWhenDeviceConnectedKey,
        false
    )
    val binder = LocalPlayerServiceBinder.current
    var isShowingSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    val sleepTimerMillisLeft by (binder?.sleepTimerMillisLeft
        ?: flowOf(null))
        .collectAsState(initial = null)

    SettingsScreenLayout(
        title = stringResource(id = R.string.player),
        onBackClick = onBackClick,
        content = {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingColum(
                    icon = IconSource.Vector(Icons.Default.Timer),
                    title = "Sleep Timer",
                    description = "Set playback duration",
                    onClick = { isShowingSleepTimerDialog = true },
                )
                SwitchSetting(
                    icon = Icons.Default.MusicOff,
                    title = stringResource(id = R.string.skip_silence),
                    description = stringResource(id = R.string.skip_silence_description),
                    switchState = skipSilence,
                    onSwitchChange = {
                        skipSilence = it
                    }
                )
                SwitchSetting(
                    painterRes = R.drawable.audio_focus,
                    title = "Audio Focus",
                    description = "Pause playback when other media is playing",
                    switchState = audioFocus,
                    onSwitchChange = { audioFocus = it }
                )
                SwitchSetting(
                    icon = Icons.Default.Pause,
                    title = "Auto Pause",
                    description = "Pause playback when volume is muted",
                    switchState = autoPauseEnabled,
                    onSwitchChange = { autoPauseEnabled = it }
                )

                if (isAtLeastAndroid6) {
                    SwitchSetting(
                        painterRes = R.drawable.headphone,
                        title = stringResource(id = R.string.resume_playback),
                        description = stringResource(id = R.string.resume_playback_description),
                        switchState = resumePlaybackWhenDeviceConnected,
                        onSwitchChange = {
                            resumePlaybackWhenDeviceConnected = it
                        }
                    )
                }
            }

            if (isShowingSleepTimerDialog) {
                SleepTimer(
                    sleepTimerMillisLeft = sleepTimerMillisLeft,
                    onDismiss = { isShowingSleepTimerDialog = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Audio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SliderSettingsItem(
                    label = "Play speed",
                    value = playSpeed,
                    onValueChange = { playSpeed = it },
                    valueRange = 0.5f..2.0f,
                    valueLabel = {
                        String.format(Locale.US, "%.1fx", it)
                    }
                )

                SliderSettingsItem(
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
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = stringResource(id = R.string.loudness_normalization),
                    description = stringResource(id = R.string.loudness_normalization_description),
                    switchState = volumeNormalization,
                    onSwitchChange = {
                        volumeNormalization = it
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    )
}