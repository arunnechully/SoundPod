package com.github.soundpod.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Song
import com.github.soundpod.models.SortBy
import com.github.soundpod.ui.items.LocalSongItem
import com.github.soundpod.utils.asMediaItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T : SortBy> SongListContent(
    songs: List<Song>,
    sortBy: T,
    onSortByChange: (T) -> Unit,
    sortByEntries: List<T>,
    onSongClick: (Int) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier,
    showThumbnail: Boolean = true,
    currentPlayingId : String? = null,
    leadingContent : (@Composable (index: Int, isPlaying: Boolean) -> Unit)? = null,
    showMoreVert: Boolean = true,
    isEditMode: Boolean = false,
    onEditModeChange: (Boolean) -> Unit = {},
    selectedUids: Set<String> = emptySet(),
    onSelectedUidsChange: (Set<String>) -> Unit = {},
    onGoToAlbum: ((String) -> Unit)? = null,
    onGoToArtist: ((String) -> Unit)? = null,
    lazyListState: LazyListState = rememberLazyListState(),
    reorderableState: ReorderableLazyListState? = null,
) {
    val playerPadding = LocalPlayerPadding.current
    val (colorPalette) = LocalAppearance.current
    val menuState = LocalMenuState.current

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp + playerPadding),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "header") {
            SortingHeader(
                sortBy = sortBy,
                changeSortBy = onSortByChange,
                sortByEntries = sortByEntries,
                onPlayClick = onPlayAll,
                onShuffleClick = onShuffleAll
            )
        }
        itemsIndexed(
            songs, key = { _, song -> song.id }
        ) { index, song ->
            val isPlaying = song.id == currentPlayingId
            val isChecked = selectedUids.contains(song.id)

            val toggleSelection = {
                val newSelection = if (isChecked) selectedUids - song.id else selectedUids + song.id
                onSelectedUidsChange(newSelection)
                if (newSelection.isEmpty()) {
                    onEditModeChange(false)
                }
            }

            val highlightColor = if (isPlaying) {
                colorPalette.accent
            } else {
                colorPalette.text
            }

            val content = @Composable { dragHandle: Modifier ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isEditMode,
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp, end = 4.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isChecked) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isChecked) MaterialTheme.colorScheme.primary else colorPalette.text.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clickable { toggleSelection() },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isChecked,
                                enter = scaleIn() + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        LocalSongItem(
                            song = song,
                            showThumbnail = showThumbnail,
                            titleColor = highlightColor,
                            showMoreVert = showMoreVert && !isEditMode,
                            leadingContent = if (leadingContent != null) {
                                { leadingContent(index, isPlaying) }
                            } else null,
                            onClick = {
                                if (isEditMode) {
                                    toggleSelection()
                                } else {
                                    onSongClick(index)
                                }
                            },
                            onLongClick = {
                                if (!isEditMode) {
                                    onEditModeChange(true)
                                    onSelectedUidsChange(setOf(song.id))
                                    onSongLongClick(song)
                                }
                            },
                            trailingContent = {
                                if (isEditMode && reorderableState != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .then(dragHandle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircleDragHandle()
                                    }
                                } else if (!isEditMode && showMoreVert) {
                                    IconButton(
                                        onClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    mediaItem = song.asMediaItem,
                                                    onDismiss = menuState::hide,
                                                    onGoToAlbum = { onGoToAlbum?.invoke(it) },
                                                    onGoToArtist = { onGoToArtist?.invoke(it) }
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.MoreVert,
                                            contentDescription = "Menu",
                                            tint = colorPalette.text
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (reorderableState != null) {
                ReorderableItem(
                    state = reorderableState,
                    key = song.id
                ) {
                    content(Modifier.draggableHandle())
                }
            } else {
                content(Modifier)
            }
        }
    }
}
