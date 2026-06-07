@file:Suppress("AssignedValueIsNeverRead")

package com.github.soundpod.ui.screens.settings

import android.annotation.SuppressLint
import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ManageHistory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.github.soundpod.db
import com.github.soundpod.enums.CoilDiskCacheMaxSize
import com.github.soundpod.enums.ExoPlayerDiskCacheMaxSize
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.query
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.utils.ScreenCache
import com.github.soundpod.utils.coilDiskCacheMaxSizeKey
import com.github.soundpod.utils.exoPlayerDiskCacheMaxSizeKey
import com.github.soundpod.utils.isScreenCacheEnabledKey
import com.github.soundpod.utils.pauseSearchHistoryKey
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.quickPicksSourceKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.showCachedSongsInOfflineKey
import kotlinx.coroutines.flow.distinctUntilChanged

@SuppressLint("LocalContextGetResourceValueCall")
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
    var pauseSearchHistory by rememberPreference(pauseSearchHistoryKey, false)

    var pauseSongCache by rememberPreference(pauseSongCacheKey, false)
    var showCachedSongsInOffline by rememberPreference(showCachedSongsInOfflineKey, true)
    var isScreenCacheEnabled by rememberPreference(isScreenCacheEnabledKey, true)

    val eventsCount by remember {
        db.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    var quickPicksSource by rememberPreference(quickPicksSourceKey, QuickPicksSource.Trending)

    var showClearQuickPicksDialog by remember { mutableStateOf(false) }
    var showClearScreenCacheDialog by remember { mutableStateOf(false) }
    var showClearImageCacheDialog by remember { mutableStateOf(false) }
    var showClearSongCacheDialog by remember { mutableStateOf(false) }

    var refreshTrigger by remember { mutableIntStateOf(0) }

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
                isEnabled = eventsCount > 0,
            )
        }

        if (showClearQuickPicksDialog) {
            SettingsAlertDialog(
                title = stringResource(id = R.string.reset_quick_picks),
                onDismissRequest = { showClearQuickPicksDialog = false },
                onConfirmClick = {
                    query(db::clearEvents)
                    showClearQuickPicksDialog = false
                },
                alertMessage = stringResource(id = R.string.reset_quick_picks_alert)
            )
        }

        SettingsGroup(
            title = stringResource(id = R.string.screen_cache)
        ) {
            SwitchSetting(
                icon = IconSource.Vector(Icons.Outlined.Store),
                title = stringResource(id = R.string.screen_cache),
                description = stringResource(id = R.string.screen_cache_description),
                switchState = isScreenCacheEnabled,
                onSwitchChange = { isScreenCacheEnabled = it }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { showClearScreenCacheDialog = true },
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

        if (showClearScreenCacheDialog) {
            SettingsAlertDialog(
                title = stringResource(id = R.string.screen_cache),
                onDismissRequest = { showClearScreenCacheDialog = false },
                onConfirmClick = {
                    ScreenCache.preferences.edit { clear() }
                    showClearScreenCacheDialog = false
                },
                alertMessage = stringResource(id = R.string.clear_screen_cache)
            )
        }

        SettingsGroup(
            title = stringResource(id = R.string.image_cache)
        ) {
            val (colorPalette) = LocalAppearance.current
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
        val (colorPalette) = LocalAppearance.current

        SettingsGroup(
            title = stringResource(id = R.string.audio_cache)
        ) {
            SwitchSetting(
                icon = IconSource.Vector(Icons.Outlined.RemoveRedEye),
                title = stringResource(id = R.string.show_cached_songs),
                description = stringResource(id = R.string.show_cached_songs_description),
                switchState = showCachedSongsInOffline,
                onSwitchChange = { showCachedSongsInOffline = it }
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
        SettingsInformation(text = stringResource(id = R.string.cache_information))
    }
}
