package com.github.soundpod.ui.screens.settings.subscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.query
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.items.LocalSongItem
import com.github.soundpod.ui.screens.settings.SettingsAlertDialog
import com.github.soundpod.ui.screens.settings.SettingsColumn
import com.github.soundpod.ui.screens.settings.SettingsGroup
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun BlacklistSettingsContent() {
    val (colorPalette) = LocalAppearance.current
    val blacklistedSongs by remember {
        db.blacklistedSongs().distinctUntilChanged()
    }.collectAsState(initial = emptyList())

    var showClearDialog by remember { mutableStateOf(false) }

    Column {
        if (blacklistedSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        tint = colorPalette.text.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_blacklisted_songs),
                        style = typography.titleMedium,
                        color = colorPalette.text
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.no_blacklisted_songs_description),
                        style = typography.bodyMedium,
                        color = colorPalette.text.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            SettingsGroup {
                SettingsColumn(
                    icon = IconSource.Vector(Icons.Outlined.DeleteSweep),
                    title = stringResource(R.string.clear_blacklist),
                    onClick = { showClearDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                Column {
                    blacklistedSongs.forEach { song ->
                        LocalSongItem(
                            song = song,
                            onClick = {},
                            onLongClick = {},
                            showMoreVert = false,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        query {
                                            db.blacklist(song.id, null)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Restore,
                                        contentDescription = stringResource(R.string.retrieve),
                                        tint = colorPalette.text
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showClearDialog) {
            SettingsAlertDialog(
                title = stringResource(R.string.clear_blacklist),
                onDismissRequest = { showClearDialog = false },
                onConfirmClick = {
                    query(db::clearBlacklist)
                    showClearDialog = false
                },
                alertMessage = stringResource(R.string.clear_blacklist_confirm)
            )
        }
    }
}
