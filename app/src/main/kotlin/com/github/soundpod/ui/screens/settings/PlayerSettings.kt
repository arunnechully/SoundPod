package com.github.soundpod.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.utils.persistentQueueKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.resumePlaybackWhenDeviceConnectedKey
import com.github.soundpod.utils.skipSilenceKey
import com.github.soundpod.utils.volumeNormalizationKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    onBackClick: () -> Unit
) {
    var skipSilence by rememberPreference(skipSilenceKey, false)
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)
    var resumePlaybackWhenDeviceConnected by rememberPreference(
        resumePlaybackWhenDeviceConnectedKey,
        false
    )
    var persistentQueue by rememberPreference(persistentQueueKey, false)

    BackHandler(onBack = onBackClick)

    SettingsScreenLayout(
        title = stringResource(id = R.string.player),
        shape = MaterialTheme.shapes.extraSmall,
        onBackClick = onBackClick,
        content = {

            SettingsGroup(
                title = stringResource(R.string.play_back)
            ) {
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
        }
    )
}