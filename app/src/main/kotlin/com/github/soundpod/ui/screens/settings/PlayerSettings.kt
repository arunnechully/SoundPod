package com.github.soundpod.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SliderSettingsItem
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.utils.formatAsDuration
import com.github.soundpod.utils.pauseOnAppCloseKey
import com.github.soundpod.utils.persistentQueueKey
import com.github.soundpod.utils.playbackPitchKey
import com.github.soundpod.utils.playbackSpeedKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.resumePlaybackWhenDeviceConnectedKey
import com.github.soundpod.utils.skipSilenceKey
import com.github.soundpod.utils.stopAfterCurrentKey
import com.github.soundpod.utils.volumeNormalizationKey
import java.util.Locale

@Composable
fun PlayerSettingsContent(
    onSleepTimerClick: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val sleepTimerMillisLeft by (binder?.sleepTimerMillisLeft
        ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    val stopAfterCurrent by rememberPreference(stopAfterCurrentKey, false)

    var skipSilence by rememberPreference(skipSilenceKey, false)
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)
    var resumePlaybackWhenDeviceConnected by rememberPreference(
        resumePlaybackWhenDeviceConnectedKey,
        false
    )
    var persistentQueue by rememberPreference(persistentQueueKey, false)
    var playSpeed by rememberPreference(playbackSpeedKey, 1f)
    var playPitch by rememberPreference(playbackPitchKey, 1f)
    var pauseOnAppClose by rememberPreference(pauseOnAppCloseKey, false)

    SettingsGroup(
        title = stringResource(R.string.play_back)
    ) {
        SettingsColumn(
            icon = IconSource.Vector(Icons.Outlined.Timer),
            title = stringResource(R.string.sleep_timer),
            description = when {
                stopAfterCurrent && sleepTimerMillisLeft != null ->
                    "${stringResource(R.string.stop_after_current)} • ${
                        formatAsDuration(
                            sleepTimerMillisLeft!!
                        )
                    }"

                stopAfterCurrent -> stringResource(R.string.stop_after_current)
                sleepTimerMillisLeft != null -> formatAsDuration(sleepTimerMillisLeft!!)
                else -> stringResource(R.string.off)
            },
            descriptionColor = LocalAppearance.current.colorPalette.accent,
            onClick = onSleepTimerClick,
        )
    }
    SettingsGroup {
        SwitchSetting(
            icon = IconSource.Vector(Icons.AutoMirrored.Outlined.QueueMusic),
            title = stringResource(id = R.string.persistent_queue),
            description = stringResource(id = R.string.persistent_queue_description),
            switchState = persistentQueue,
            onSwitchChange = {
                persistentQueue = it
            }
        )

        SwitchSetting(
            icon = IconSource.Vector(Icons.Default.MusicOff),
            title = stringResource(id = R.string.skip_silence),
            description = stringResource(id = R.string.skip_silence_description),
            switchState = skipSilence,
            onSwitchChange = {
                skipSilence = it
            }
        )
        SwitchSetting(
            icon = IconSource.Icon(painterResource(R.drawable.headphone)),
            title = stringResource(id = R.string.resume_playback),
            description = stringResource(id = R.string.resume_playback_description),
            switchState = resumePlaybackWhenDeviceConnected,
            onSwitchChange = {
                resumePlaybackWhenDeviceConnected = it
            }
        )

        SwitchSetting(
            icon = IconSource.Vector(Icons.AutoMirrored.Outlined.ExitToApp),
            title = stringResource(R.string.stop_on_app_close),
            description = stringResource(R.string.stop_on_app_close_description),
            switchState = pauseOnAppClose,
            onSwitchChange = {
                pauseOnAppClose = it
            }
        )
    }

    SettingsGroup(
        title = stringResource(R.string.audio)
    ) {
        SwitchSetting(
            icon = IconSource.Vector(Icons.AutoMirrored.Filled.VolumeUp),
            title = stringResource(id = R.string.loudness_normalization),
            description = stringResource(id = R.string.loudness_normalization_description),
            switchState = volumeNormalization,
            onSwitchChange = {
                volumeNormalization = it
            }
        )
    }

    SettingsGroup(
        title = stringResource(R.string.advanced)
    ) {
        SliderSettingsItem(
            label = stringResource(R.string.play_back) + " speed",
            value = playSpeed,
            onValueChange = { playSpeed = it },
            valueRange = 0.5f..2.0f,
            valueLabel = { String.format(Locale.US, "%.1fx", it) },
            defaultValue = 1f,
            hapticUseIntegerStep = false,
            hapticUseFloatStep = true,
            hapticFloatStep = 0.1f
        )

        SliderSettingsItem(
            label = stringResource(R.string.play_pitch),
            value = playPitch,
            onValueChange = { playPitch = it },
            valueRange = 0.5f..2.0f,
            valueLabel = { String.format(Locale.US, "%.1fx", it) },
            defaultValue = 1f,
            hapticUseIntegerStep = false,
            hapticUseFloatStep = true,
            hapticFloatStep = 0.1f
        )
    }
}
