@file:Suppress("AssignedValueIsNeverRead")

package com.github.soundpod.ui.screens.settings.newsettings

import android.text.format.Formatter
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil3.imageLoader
import com.github.soundpod.Database
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.enums.CoilDiskCacheMaxSize
import com.github.soundpod.enums.ExoPlayerDiskCacheMaxSize
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.query
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.screens.settings.EnumValueSelectorSettingsEntry
import com.github.soundpod.ui.screens.settings.SettingsInformation
import com.github.soundpod.ui.screens.settings.SettingsProgress
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.coilDiskCacheMaxSizeKey
import com.github.soundpod.utils.exoPlayerDiskCacheMaxSizeKey
import com.github.soundpod.utils.pauseSearchHistoryKey
import com.github.soundpod.utils.quickPicksSourceKey
import com.github.soundpod.utils.rememberPreference
import kotlinx.coroutines.flow.distinctUntilChanged

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheSettings(
    onBackClick: () -> Unit
) {

    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

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

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    val eventsCount by remember {
        Database.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    var quickPicksSource by rememberPreference(quickPicksSourceKey, QuickPicksSource.Trending)

    SettingsScreenLayout(
        title =stringResource(id = R.string.database),
        onBackClick = onBackClick,
        content = {

            SettingsCard {

                SwitchSetting(
                    textColor = textColor,
                    icon = Icons.Outlined.HistoryToggleOff,
                    title = stringResource(id = R.string.pause_search_history),
                    description = stringResource(id = R.string.pause_search_history_description),
                    switchState = pauseSearchHistory,
                    onSwitchChange = { pauseSearchHistory = it }
                )

                SettingColum(
                    textColor = textColor,
                    icon = Icons.Outlined.DeleteSweep,
                    title = stringResource(id = R.string.clear_search_history),
                    description = if (queriesCount > 0) {
                        stringResource(id = R.string.delete_search_queries, queriesCount)
                    } else {
                        stringResource(id = R.string.history_is_empty)
                    },
                    onClick = { query(Database::clearQueries) },
                    isEnabled = queriesCount > 0
                )

                EnumValueSelectorSettingsEntry(
                    title = stringResource(id = R.string.quick_picks_source),
                    selectedValue = quickPicksSource,
                    onValueSelected = { quickPicksSource = it },
                    icon = Icons.Default.AutoAwesome,
                    valueText = { context.getString(it.resourceId) }
                )

                SettingColum(
                    textColor = textColor,
                    icon = Icons.Outlined.RestartAlt,
                    title = stringResource(id = R.string.reset_quick_picks),
                    description = if (eventsCount > 0) {
                        stringResource(id = R.string.delete_playback_events, eventsCount)
                    } else {
                        stringResource(id = R.string.quick_picks_cleared)
                    },
                    onClick = { query(Database::clearEvents) },
                    isEnabled = eventsCount > 0
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsCard {
                context.imageLoader.diskCache?.let { diskCache ->
                    val diskCacheSize = remember(diskCache) {
                        diskCache.size
                    }

                    Text(
                        text = stringResource(id = R.string.image_cache),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )

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
                        icon = Icons.Outlined.Image
                    )
                }

                binder?.cache?.let { cache ->
                    val diskCacheSize by remember {
                        derivedStateOf {
                            cache.cacheSpace
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimensions.spacer))

                    Text(
                        text = stringResource(id = R.string.song_cache),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )

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
                        icon = Icons.Outlined.MusicNote
                    )
                }
            }
            SettingsInformation(text = stringResource(id = R.string.cache_information))
        }
    )
}