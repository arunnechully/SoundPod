package com.github.soundpod.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.HistorySortBy
import com.github.soundpod.ui.components.ConfirmationDialog
import com.github.soundpod.ui.components.SongListContent
import com.github.soundpod.ui.components.StaticScreenLayout
import com.github.soundpod.ui.components.TextFieldDialog
import com.github.soundpod.models.Song
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex
import com.github.soundpod.utils.historySortByKey
import com.github.soundpod.utils.historySortOrderKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.viewmodels.home.HistoryViewModel
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalAnimationApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun HistoryScreen(
    onBackClick: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val binder = LocalPlayerServiceBinder.current

    var isEditMode by remember { mutableStateOf(false) }
    var selectedUids by remember { mutableStateOf(emptySet<String>()) }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    var sortBy by rememberPreference(historySortByKey, HistorySortBy.Recent)
    var sortOrder by rememberPreference(historySortOrderKey, SortOrder.Descending)

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index == 0 || to.index == 0) return@rememberReorderableLazyListState
        viewModel.move(from.index - 1, to.index - 1)
    }

    BackHandler(enabled = isEditMode) {
        isEditMode = false
        selectedUids = emptySet()
    }

    LaunchedEffect(sortBy) {
        viewModel.changeSortBy(sortBy)
    }
    LaunchedEffect(sortOrder) {
        viewModel.changeSortOrder(sortOrder)
    }

    if (showCreatePlaylistDialog) {
        TextFieldDialog(
            title = stringResource(id = R.string.new_playlist),
            hintText = stringResource(id = R.string.playlist_name_hint),
            initialTextInput = stringResource(id = R.string.history),
            onDismiss = { showCreatePlaylistDialog = false },
            onDone = { name ->
                val songIds = if (selectedUids.isNotEmpty()) {
                    // Filter the ordered list by selection
                    viewModel.historySongs.filter { it.id in selectedUids }.map { it.id }
                } else {
                    viewModel.historySongs.map { it.id }
                }
                viewModel.createPlaylist(name, songIds)
                showCreatePlaylistDialog = false
                isEditMode = false
                selectedUids = emptySet()
            }
        )
    }

    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.clear_history),
            text = stringResource(R.string.clear_history_confirmation),
            onDismiss = { showClearHistoryDialog = false },
            onConfirm = {
                viewModel.clearHistory()
                showClearHistoryDialog = false
            }
        )
    }

    StaticScreenLayout(
        title = {
            if (isEditMode) {
                val isAllSelected = selectedUids.size == viewModel.historySongs.size && viewModel.historySongs.isNotEmpty()
                TextButton(
                    onClick = {
                        selectedUids = if (isAllSelected) {
                            emptySet()
                        } else {
                            viewModel.historySongs.map { it.id }.toSet()
                        }
                    }
                ) {
                    Text(
                        text = if (isAllSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all)
                    )
                }
            } else {
                Text(text = stringResource(id = R.string.history))
            }
        },
        onBackClick = {
            if (isEditMode) {
                isEditMode = false
                selectedUids = emptySet()
            } else {
                onBackClick()
            }
        },
        actions = {},
        dropDownMenuContent = { dismissMenu ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.create_playlist)) },
                onClick = {
                    dismissMenu()
                    if (isEditMode) {
                        showCreatePlaylistDialog = true
                    } else {
                        isEditMode = true
                    }
                }
            )
            if (!isEditMode) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clear_history)) },
                    onClick = {
                        dismissMenu()
                        showClearHistoryDialog = true
                    }
                )
            }
        },
        scrollable = false,
        horizontalPadding = 0.dp
    ) {
        SettingsCard {
            SongListContent(
                songs = viewModel.historySongs,
                sortBy = sortBy,
                onSortByChange = { sortBy = it },
                sortByEntries = HistorySortBy.entries,
                onSongClick = { index ->
                    val mediaItems = viewModel.historySongs.map { it.asMediaItem }
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(mediaItems, index)
                },
                onSongLongClick = { /* Handled by SongListContent internal logic for selection */ },
                onPlayAll = {
                    val mediaItems = viewModel.historySongs.map { it.asMediaItem }
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(mediaItems, 0)
                },
                onShuffleAll = {
                    val mediaItems = viewModel.historySongs.shuffled().map { it.asMediaItem }
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(mediaItems, 0)
                },
                isEditMode = isEditMode,
                onEditModeChange = { isEditMode = it },
                selectedUids = selectedUids,
                onSelectedUidsChange = { selectedUids = it },
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                lazyListState = lazyListState,
                reorderableState = reorderableState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
