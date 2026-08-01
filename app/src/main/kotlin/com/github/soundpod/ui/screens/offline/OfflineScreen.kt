package com.github.soundpod.ui.screens.offline

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.Song
import com.github.soundpod.query
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.StaticScreenLayout
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.songSortByKey
import com.github.soundpod.utils.songSortOrderKey

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun OfflineScreen(
    builtInPlaylist: BuiltInPlaylist,
    pop: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current

    var isEditMode by remember { mutableStateOf(false) }
    var selectedUids by remember { mutableStateOf(emptySet<String>()) }
    var isSearching by remember { mutableStateOf(false) }

    var currentSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    var sortBy by rememberPreference(songSortByKey, SongSortBy.Title)
    var sortOrder by rememberPreference(songSortOrderKey, SortOrder.Ascending)

    var removeSongDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isEditMode || isSearching) {
        if (isSearching) {
            isSearching = false
        } else if (isEditMode) {
            isEditMode = false
            selectedUids = emptySet()
        }
    }

    StaticScreenLayout(
        scrollable = false,
        horizontalPadding = 0.dp,
        title = {
            if (isEditMode) {
                val isAllSelected = selectedUids.size == currentSongs.size && currentSongs.isNotEmpty()

                TextButton(
                    onClick = {
                        selectedUids = if (isAllSelected) {
                            emptySet()
                        } else {
                            currentSongs.map { it.id }.toSet()
                        }
                    }
                ) {
                    Text(
                        text = if (isAllSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.offline)
                )
            }
        },
        onBackClick = {
            if (isSearching) {
                isSearching = false
            } else if (isEditMode) {
                isEditMode = false
                selectedUids = emptySet()
            } else {
                pop()
            }
        },
        actions = {
                AnimatedVisibility(
                    visible = isEditMode,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    IconButton(
                        onClick = { removeSongDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.remove_from_favorites)
                        )
                    }
                }

            if (removeSongDialog) {
                AlertDialog(
                    onDismissRequest = { removeSongDialog = false },
                    title = {
                        Text(text = stringResource(R.string.clear_playlist))
                    },
                    text = {
                        Text(text = stringResource(id =R.string.remove_songs_confirmation))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (isEditMode) {
                                    val uidsToDelete = selectedUids.toList()
                                    query {
                                        db.removeSongsFromPlaylist(builtInPlaylist, uidsToDelete)
                                    }
                                    if (builtInPlaylist == BuiltInPlaylist.Offline) {
                                        binder?.removeFromCache(uidsToDelete)
                                    }
                                    isEditMode = false
                                    selectedUids = emptySet()
                                } else {
                                    query { db.clearPlaylist(builtInPlaylist) }
                                    if (builtInPlaylist == BuiltInPlaylist.Offline) {
                                        binder?.clearCache()
                                    }
                                }
                                removeSongDialog = false
                            }
                        ) {
                            Text(text = stringResource(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { removeSongDialog = false }
                        ) {
                            Text(text = stringResource(android.R.string.cancel))
                        }
                    }
                )
            }

            IconButton(
                onClick = onSearchClick
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
        },
        dropDownMenuContent = { dismissMenu ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    onSettingsClick()
                    dismissMenu()
                }
            )
        }
    ) {
            SettingsCard(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                shape = RoundedCornerShape(
                    topStart = 25.dp,
                    topEnd = 25.dp
                )
            ) {
                OfflineSongs(
                    builtInPlaylist = builtInPlaylist,
                    isEditMode = isEditMode,
                    onEditModeChange = { isEditMode = it },
                    selectedUids = selectedUids,
                    onSelectedUidsChange = { selectedUids = it },
                    onGoToAlbum = onGoToAlbum,
                    onGoToArtist = onGoToArtist,
                    sortBy = sortBy,
                    onSortByChange = { sortBy = it },
                    sortOrder = sortOrder,
                    onSongsChange = { currentSongs = it }
                )
            }
    }
}
