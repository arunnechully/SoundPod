@file:Suppress("AssignedValueIsNeverRead")

package com.github.soundpod.ui.screens.settings

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil3.imageLoader
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.enums.CoilDiskCacheMaxSize
import com.github.soundpod.enums.ExoPlayerDiskCacheMaxSize
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.utils.coilDiskCacheMaxSizeKey
import com.github.soundpod.utils.exoPlayerDiskCacheMaxSizeKey
import com.github.soundpod.utils.pauseImageCacheKey
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.showCachedSongsInOfflineKey

@OptIn(UnstableApi::class)
@Composable
fun CacheSettingsContent() {

    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val (colorPalette) = LocalAppearance.current

    var coilDiskCacheMaxSize by rememberPreference(
        coilDiskCacheMaxSizeKey,
        CoilDiskCacheMaxSize.`128MB`
    )
    var exoPlayerDiskCacheMaxSize by rememberPreference(
        exoPlayerDiskCacheMaxSizeKey,
        ExoPlayerDiskCacheMaxSize.`2GB`
    )

    var pauseSongCache by rememberPreference(pauseSongCacheKey, false)
    var pauseImageCache by rememberPreference(pauseImageCacheKey, false)

    var showCachedSongsInOffline by rememberPreference(showCachedSongsInOfflineKey, true)

    var showClearImageCacheDialog by remember { mutableStateOf(false) }
    var showClearSongCacheDialog by remember { mutableStateOf(false) }

    var refreshTrigger by remember { mutableIntStateOf(0) }

    Column {
        SettingsGroup(
            title = stringResource(id = R.string.audio_cache)
        ) {
            SwitchSetting(
                icon = IconSource.Icon(painterResource(id = R.drawable.music_file)),
                title = stringResource(id = R.string.pause_song_cache),
                description = stringResource(id = R.string.pause_song_cache_description),
                switchState = pauseSongCache,
                onSwitchChange = { pauseSongCache = it }
            )
        }
        SettingsGroup {
            binder?.cache?.let { cache ->
                val diskCacheSize = remember(cache, refreshTrigger) {
                    cache.cacheSpace
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsProgress(
                        modifier = Modifier.weight(1f),
                        text = Formatter.formatShortFileSize(
                            context,
                            diskCacheSize
                        ),
                        progress = when (val size = exoPlayerDiskCacheMaxSize) {
                            ExoPlayerDiskCacheMaxSize.Unlimited -> 0F
                            else -> (diskCacheSize.toFloat() / size.bytes.toFloat())
                        }
                    )

                    TextButton(
                        onClick = { showClearSongCacheDialog = true },
                        modifier = Modifier.padding(start = 8.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = colorPalette.accent.copy(alpha = 0.1f),
                            contentColor = colorPalette.accent
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.clear_cache),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            EnumValueSelectorSettingsEntry(
                title = stringResource(id = R.string.max_size),
                selectedValue = exoPlayerDiskCacheMaxSize,
                onValueSelected = { exoPlayerDiskCacheMaxSize = it },
                icon = IconSource.Vector(Icons.Outlined.MusicNote)
            )
            SwitchSetting(
                icon = IconSource.Vector(Icons.Outlined.RemoveRedEye),
                title = stringResource(id = R.string.show_cached_songs),
                description = stringResource(id = R.string.show_cached_songs_description),
                switchState = showCachedSongsInOffline,
                onSwitchChange = { showCachedSongsInOffline = it }
            )

        }

        if (showClearSongCacheDialog) {

            SettingsAlertDialog(
                title = stringResource(id = R.string.clear_cache),
                onDismissRequest = { showClearSongCacheDialog = false },
                onConfirmClick = {
                    binder?.cache?.let { cache ->
                        cache.keys.forEach { cache.removeResource(it) }
                    }
                    refreshTrigger++
                    showClearSongCacheDialog = false
                },
                alertMessage = stringResource(id = R.string.clear_audio_cache)
            )
        }

        SettingsGroup(
            title = stringResource(id = R.string.image_cache)
        ) {
            SwitchSetting(
                icon = IconSource.Icon(painterResource(id = R.drawable.image_file)),
                title = stringResource(id = R.string.pause_image_cache),
                description = stringResource(id = R.string.pause_image_cache_description),
                switchState = pauseImageCache,
                onSwitchChange = { pauseImageCache = it }
            )
        }

        SettingsGroup {
            context.imageLoader.diskCache?.let { diskCache ->
                val diskCacheSize = remember(diskCache, refreshTrigger) {
                    diskCache.size
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsProgress(
                        modifier = Modifier.weight(1f),
                        text = Formatter.formatShortFileSize(
                            context,
                            diskCacheSize
                        ),
                        progress = diskCacheSize.toFloat() / coilDiskCacheMaxSize.bytes.coerceAtLeast(
                            minimumValue = 1
                        ).toFloat()
                    )

                    TextButton(
                        onClick = { showClearImageCacheDialog = true },
                        modifier = Modifier.padding(start = 8.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = colorPalette.accent.copy(alpha = 0.1f),
                            contentColor = colorPalette.accent
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.clear_cache),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            EnumValueSelectorSettingsEntry(
                title = stringResource(id = R.string.max_size),
                selectedValue = coilDiskCacheMaxSize,
                onValueSelected = { coilDiskCacheMaxSize = it },
                icon = IconSource.Vector(Icons.Outlined.Image)
            )
        }

        if (showClearImageCacheDialog) {

            SettingsAlertDialog(
                title = stringResource(id = R.string.clear_cache),
                onDismissRequest = { showClearImageCacheDialog = false },
                onConfirmClick = {
                    context.imageLoader.diskCache?.clear()
                    refreshTrigger++
                    showClearImageCacheDialog = false
                },
                alertMessage = stringResource(id = R.string.clear_image_cache)
            )
        }
        SettingsInformation(text = stringResource(id = R.string.cache_information))
    }
}
