package com.github.soundpod.ui.screens.builtinplaylist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.db
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Song
import com.github.soundpod.ui.components.CircleDragHandle
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.SortingHeader
import com.github.soundpod.ui.items.LocalSongItem
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.Collections

@ExperimentalAnimationApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun NewBuiltInPlaylistSongs(
    builtInPlaylist: BuiltInPlaylist,
    isEditMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    selectedUids: Set<String>,
    onSelectedUidsChange: (Set<String>) -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    sortBy: SongSortBy,
    onSortByChange: (SongSortBy) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    onSongsChange: (List<Song>) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val (colorPalette) = LocalAppearance.current
    val playerPadding = LocalPlayerPadding.current

    var songs: List<Song> by remember { mutableStateOf(emptyList()) }

    val lazyListState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index == 0 || to.index == 0) return@rememberReorderableLazyListState
        val mutableSongs = songs.toMutableList()
        Collections.swap(mutableSongs, from.index - 1, to.index - 1)
        songs = mutableSongs
    }

    LaunchedEffect(builtInPlaylist, sortBy, sortOrder) {
        when (builtInPlaylist) {
            BuiltInPlaylist.Favorites -> {
                db.favorites()
                    .map { favSongs ->
                        when (sortBy) {
                            SongSortBy.Title -> if (sortOrder == SortOrder.Ascending) favSongs.sortedBy { it.title } else favSongs.sortedByDescending { it.title }
                            SongSortBy.PlayTime -> if (sortOrder == SortOrder.Ascending) favSongs.sortedBy { it.totalPlayTimeMs } else favSongs.sortedByDescending { it.totalPlayTimeMs }
                            SongSortBy.DateAdded -> if (sortOrder == SortOrder.Ascending) favSongs.sortedBy { it.likedAt } else favSongs.sortedByDescending { it.likedAt }
                            SongSortBy.Artist -> if (sortOrder == SortOrder.Ascending) favSongs.sortedBy { it.artistsText.toString() } else favSongs.sortedByDescending { it.artistsText.toString() }
                        }
                    }
                    .flowOn(Dispatchers.IO)
            }
            BuiltInPlaylist.Offline -> {
                db.songs(sortBy, sortOrder)
                    .map { sortedSongs ->
                        sortedSongs.filter { item ->
                            binder?.cache?.isCached(item.id, 0, Long.MAX_VALUE) ?: false
                        }
                    }
                    .flowOn(Dispatchers.IO)
            }
        }.collect {
            songs = it
            onSongsChange(it)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(top = 0.dp, bottom = 16.dp + playerPadding),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "header") {
                SortingHeader(
                    sortBy = sortBy,
                    changeSortBy = onSortByChange,
                    sortByEntries = SongSortBy.entries.toList(),
                    sortOrder = sortOrder,
                    toggleSortOrder = { onSortOrderChange(!sortOrder) },
                    size = songs.size,
                    onPlayClick = {
                        binder?.stopRadio()
                        binder?.player?.forcePlayAtIndex(songs.map(Song::asMediaItem), 0)
                    },
                    onShuffleClick = {
                        binder?.stopRadio()
                        val shuffledSongs = songs.shuffled()
                        binder?.player?.forcePlayAtIndex(shuffledSongs.map(Song::asMediaItem), 0)
                    }
                )
            }

            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                val isChecked = selectedUids.contains(song.id)

                // Centralized selection toggle logic
                val toggleSelection = {
                    val newSelection = if (isChecked) selectedUids - song.id else selectedUids + song.id
                    onSelectedUidsChange(newSelection)
                    if (newSelection.isEmpty()) {
                        onEditModeChange(false)
                    }
                }

                ReorderableItem(
                    state = reorderableState,
                    key = song.id
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
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
                                SongsAnimatedVisibility(
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
                                onClick = {
                                    if (isEditMode) {
                                        toggleSelection()
                                    } else {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(
                                            songs.map(Song::asMediaItem),
                                            index
                                        )
                                    }
                                },
                                onLongClick = {
                                    if (!isEditMode) {
                                        onEditModeChange(true)
                                        onSelectedUidsChange(setOf(song.id))
                                    }
                                },
                                trailingContent = {
                                    if (isEditMode) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .draggableHandle(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircleDragHandle()
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                menuState.display {
                                                    NonQueuedMediaItemMenu(
                                                        mediaItem = song.asMediaItem,
                                                        onDismiss = menuState::hide,
                                                        onGoToAlbum = onGoToAlbum,
                                                        onGoToArtist = onGoToArtist
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
            }
        }
        if (songs.isEmpty()) {
            Text(
                text = "No songs available",
                style = MaterialTheme.typography.bodyLarge,
                color = colorPalette.text.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun SongsAnimatedVisibility(
    visible: Boolean,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        content = content
    )
}