package com.github.soundpod.ui.screens.offline

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.ContentMetadata
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.db
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.Song
import com.github.soundpod.ui.components.SongListContent
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.showCachedSongsInOfflineKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.map
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.Collections

@ExperimentalAnimationApi
@OptIn(UnstableApi::class)
@Composable
fun OfflineSongs(
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
    onSongsChange: (List<Song>) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current

    val showCachedSongsInOffline by rememberPreference(showCachedSongsInOfflineKey, true)

    var songs: List<Song> by remember { mutableStateOf(emptyList()) }

    val lazyListState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index == 0 || to.index == 0) return@rememberReorderableLazyListState
        val mutableSongs = songs.toMutableList()
        Collections.swap(mutableSongs, from.index - 1, to.index - 1)
        songs = mutableSongs
    }

    LaunchedEffect(builtInPlaylist, sortBy, sortOrder, showCachedSongsInOffline, binder) {
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
                if (showCachedSongsInOffline) {
                    combine(
                        db.downloadedSongs(),
                        binder?.cacheChanges?.onStart { emit(Unit) } ?: flowOf(Unit)
                    ) { downloadedSongs, _ ->
                        val binderCache = binder?.cache
                        downloadedSongs
                            .filter { item ->
                                val metadata = binderCache?.getContentMetadata(item.song.id)
                                val length = metadata?.let { ContentMetadata.getContentLength(it) }?.takeIf { it > 0 }
                                    ?: item.contentLength ?: 0L

                                length > 0 && binderCache?.isCached(item.song.id, 0, length) == true
                            }.map { it.song }
                            .let { songs ->
                                when (sortBy) {
                                    SongSortBy.Title -> if (sortOrder == SortOrder.Ascending) songs.sortedBy { it.title } else songs.sortedByDescending { it.title }
                                    SongSortBy.PlayTime -> if (sortOrder == SortOrder.Ascending) songs.sortedBy { it.totalPlayTimeMs } else songs.sortedByDescending { it.totalPlayTimeMs }
                                    SongSortBy.DateAdded -> if (sortOrder == SortOrder.Ascending) songs else songs.reversed()
                                    SongSortBy.Artist -> if (sortOrder == SortOrder.Ascending) songs.sortedBy { it.artistsText.toString() } else songs.sortedByDescending { it.artistsText.toString() }
                                }
                            }
                    }
                    .flowOn(Dispatchers.IO)
                } else {
                    flowOf(emptyList())
                }
            }
        }.collect {
            songs = it
            onSongsChange(it)
        }
    }

    SongListContent(
        songs = songs,
        sortBy = sortBy,
        onSortByChange = onSortByChange,
        sortByEntries = SongSortBy.entries.toList(),
        onSongClick = { index ->
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(
                songs.map(Song::asMediaItem),
                index
            )
        },
        onSongLongClick = { /* Handled by SongListContent internal logic for selection */ },
        onPlayAll = {
            binder?.stopRadio()
            binder?.player?.forcePlayAtIndex(songs.map(Song::asMediaItem), 0)
        },
        onShuffleAll = {
            binder?.stopRadio()
            val shuffledSongs = songs.shuffled()
            binder?.player?.forcePlayAtIndex(shuffledSongs.map(Song::asMediaItem), 0)
        },
        isEditMode = isEditMode,
        onEditModeChange = onEditModeChange,
        selectedUids = selectedUids,
        onSelectedUidsChange = onSelectedUidsChange,
        onGoToAlbum = onGoToAlbum,
        onGoToArtist = onGoToArtist,
        lazyListState = lazyListState,
        reorderableState = reorderableState,
        modifier = Modifier.fillMaxSize()
    )
}
