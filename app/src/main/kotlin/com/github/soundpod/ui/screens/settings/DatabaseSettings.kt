@file:Suppress("AssignedValueIsNeverRead")

package com.github.soundpod.ui.screens.settings

import android.annotation.SuppressLint
import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ManageHistory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import coil3.imageLoader
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.CoilDiskCacheMaxSize
import com.github.soundpod.enums.ExoPlayerDiskCacheMaxSize
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.query
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.coilDiskCacheMaxSizeKey
import com.github.soundpod.utils.exoPlayerDiskCacheMaxSizeKey
import com.github.soundpod.utils.pauseSearchHistoryKey
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.quickPicksSourceKey
import com.github.soundpod.utils.rememberPreference
import kotlinx.coroutines.flow.distinctUntilChanged

@SuppressLint("LocalContextGetResourceValueCall")
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun CacheSettingsContent() {

    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current

    var coilDiskCacheMaxSize by rememberPreference(
        coilDiskCacheMaxSizeKey,
        CoilDiskCacheMaxSize.`128MB`
    )
    var exoPlayerDiskCacheMaxSize by rememberPreference(
        exoPlayerDiskCacheMaxSizeKey,
        ExoPlayerDiskCacheMaxSize.`2GB`
    )
    var pauseSearchHistory by rememberPreference(pauseSearchHistoryKey, false)

    var pauseSongCache by rememberPreference(pauseSongCacheKey, false)

    val eventsCount by remember {
        db.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    var quickPicksSource by rememberPreference(quickPicksSourceKey, QuickPicksSource.Trending)

    var showClearQuickPicksDialog by remember { mutableStateOf(false) }
    var showClearImageCacheDialog by remember { mutableStateOf(false) }
    var showClearSongCacheDialog by remember { mutableStateOf(false) }

    var refreshTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    Column {

        SettingsGroup {
            EnumValueSelectorSettingsEntry(
                title = stringResource(id = R.string.quick_picks_source),
                selectedValue = quickPicksSource,
                onValueSelected = { quickPicksSource = it },
                icon = IconSource.Vector(Icons.Default.AutoAwesome),
                valueText = { context.getString(it.resourceId) }
            )

            SwitchSetting(
                icon = IconSource.Vector(Icons.Outlined.ManageHistory),
                title = stringResource(id = R.string.pause_search_history),
                description = stringResource(id = R.string.pause_search_history_description),
                switchState = pauseSearchHistory,
                onSwitchChange = { pauseSearchHistory = it }
            )

            SwitchSetting(
                icon = IconSource.Icon(painterResource(id = R.drawable.database)),
                title = stringResource(id = R.string.pause_song_cache),
                description = stringResource(id = R.string.pause_song_cache_description),
                switchState = pauseSongCache,
                onSwitchChange = { pauseSongCache = it }
            )
        }
        SettingsGroup {
            SettingsColumn(
                icon = IconSource.Vector(Icons.Outlined.RestartAlt),
                title = stringResource(id = R.string.reset_quick_picks),
                description = if (eventsCount > 0) {
                    stringResource(id = R.string.delete_playback_events, eventsCount)
                } else {
                    stringResource(id = R.string.quick_picks_cleared)
                },
                onClick = { showClearQuickPicksDialog = true },
                isEnabled = eventsCount > 0
            )
        }

        if (showClearQuickPicksDialog) {
            AlertDialog(
                onDismissRequest = { showClearQuickPicksDialog = false },
                title = {
                    Text(text = stringResource(id = R.string.reset_quick_picks))
                },
                text = {
                    Text(text = stringResource(id = R.string.reset_quick_picks_alert))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            query(db::clearEvents)
                            showClearQuickPicksDialog = false
                        }
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearQuickPicksDialog = false }
                    ) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        SettingsGroup(
            title = stringResource(id = R.string.image_cache)
        ) {
            context.imageLoader.diskCache?.let { diskCache ->
                val diskCacheSize = remember(diskCache, refreshTrigger) {
                    diskCache.size
                }
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                SettingsProgress(
                    text = Formatter.formatShortFileSize(
                        context,
                        diskCacheSize
                    ),
                    progress = diskCacheSize.toFloat() / coilDiskCacheMaxSize.bytes.coerceAtLeast(
                        minimumValue = 1
                    ).toFloat()
                )

                EnumValueSelectorSettingsEntry(
                    title = stringResource(id = R.string.max_size),
                    selectedValue = coilDiskCacheMaxSize,
                    onValueSelected = { coilDiskCacheMaxSize = it },
                    icon = IconSource.Vector(Icons.Outlined.Image)
                )

                TextButton(
                    onClick = { showClearImageCacheDialog = true }
                ) {
                    Text(text = stringResource(id = R.string.clear_cache))
                }
            }
        }

        if (showClearImageCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearImageCacheDialog = false },
                title = {
                    Text(text = stringResource(id = R.string.clear_cache))
                },
                text = {
                    Text(text = "This process won't be able to revert back. Are you sure you want to clear the image cache?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            context.imageLoader.diskCache?.clear()
                            refreshTrigger++
                            showClearImageCacheDialog = false
                        }
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearImageCacheDialog = false }
                    ) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        SettingsGroup(
            title = stringResource(id = R.string.song_cache)
        ) {
            binder?.cache?.let { cache ->
                val diskCacheSize = remember(cache, refreshTrigger) {
                    cache.cacheSpace
                }

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                SettingsProgress(
                    text = Formatter.formatShortFileSize(
                        context,
                        diskCacheSize
                    ),
                    progress = when (val size = exoPlayerDiskCacheMaxSize) {
                        ExoPlayerDiskCacheMaxSize.Unlimited -> 0F
                        else -> (diskCacheSize.toFloat() / size.bytes.toFloat())
                    }
                )

                EnumValueSelectorSettingsEntry(
                    title = stringResource(id = R.string.max_size),
                    selectedValue = exoPlayerDiskCacheMaxSize,
                    onValueSelected = { exoPlayerDiskCacheMaxSize = it },
                    icon = IconSource.Vector(Icons.Outlined.MusicNote)
                )

                TextButton(
                    onClick = { showClearSongCacheDialog = true }
                ) {
                    Text(text = stringResource(id = R.string.clear_cache))
                }
            }
        }

        if (showClearSongCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearSongCacheDialog = false },
                title = {
                    Text(text = stringResource(id = R.string.clear_cache))
                },
                text = {
                    Text(text = "This process won't be able to revert back. Are you sure you want to clear the song cache?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            binder?.cache?.let { cache ->
                                cache.keys.forEach { cache.removeResource(it) }
                            }
                            refreshTrigger++
                            showClearSongCacheDialog = false
                        }
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearSongCacheDialog = false }
                    ) {
                        Text(text = stringResource(android.R.string.cancel))
                    }
                }
            )
        }
        SettingsInformation(text = stringResource(id = R.string.cache_information))
    }
}
