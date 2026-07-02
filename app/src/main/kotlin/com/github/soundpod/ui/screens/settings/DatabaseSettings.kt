@file:Suppress("AssignedValueIsNeverRead")

package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.ManageHistory
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.query
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.navigation.SettingsDestinations
import com.github.soundpod.utils.pauseSearchHistoryKey
import com.github.soundpod.utils.pauseSongCacheKey
import com.github.soundpod.utils.quickPicksSourceKey
import com.github.soundpod.utils.rememberPreference
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun DatabaseSettingsContent(
    onOptionClick: (String) -> Unit = {}
) {
    var pauseSearchHistory by rememberPreference(pauseSearchHistoryKey, false)

    val eventsCount by remember {
        db.eventsCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    var quickPicksSource by rememberPreference(quickPicksSourceKey, QuickPicksSource.Default)
    var showClearQuickPicksDialog by remember { mutableStateOf(false) }

    Column {
        SettingsGroup {
            SettingsColumn(
                title = stringResource(id = R.string.quick_picks_source),
                description = stringResource(id = quickPicksSource.resourceId),
                onClick = { onOptionClick(SettingsDestinations.QUICK_PICKS) },
                icon = IconSource.Vector(Icons.Default.AutoAwesome),
            )

            SwitchSetting(
                icon = IconSource.Vector(Icons.Outlined.ManageHistory),
                title = stringResource(id = R.string.pause_search_history),
                description = stringResource(id = R.string.pause_search_history_description),
                switchState = pauseSearchHistory,
                onSwitchChange = { pauseSearchHistory = it }
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
    }
}
