package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.soundpod.db
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.HistorySortBy
import com.github.soundpod.models.Playlist
import com.github.soundpod.models.Song
import com.github.soundpod.models.SongPlaylistMap
import com.github.soundpod.utils.historySortByKey
import com.github.soundpod.utils.historySortOrderKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class HistoryViewModel : ViewModel() {
    var sortBy by mutableStateOf(HistorySortBy.Recent)
    var sortOrder by mutableStateOf(SortOrder.Descending)

    private val historyFlow = db.history(limit = 100, minPlayTimeMs = MIN_HISTORY_PLAY_TIME_MS)
    private val sortByFlow = MutableStateFlow(HistorySortBy.Recent)
    private val sortOrderFlow = MutableStateFlow(SortOrder.Descending)

    var historySongs: List<Song> by mutableStateOf(emptyList())
        private set

    companion object {
        private const val MIN_HISTORY_PLAY_TIME_MS = 30000L // 30 seconds
    }

    init {
        viewModelScope.launch {
            combine(historyFlow, sortByFlow, sortOrderFlow) { songs, sortBy, sortOrder ->
                val sortedSongs = when (sortBy) {
                    HistorySortBy.Recent -> songs // Already sorted by timestamp DESC in DB
                    HistorySortBy.Title -> songs.sortedBy { it.title }
                    HistorySortBy.Artist -> songs.sortedBy { it.artistsText }
                }
                if (sortOrder == SortOrder.Descending && sortBy != HistorySortBy.Recent) {
                    sortedSongs.reversed()
                } else if (sortOrder == SortOrder.Ascending && sortBy == HistorySortBy.Recent) {
                    sortedSongs.reversed()
                } else {
                    sortedSongs
                }
            }.collect {
                historySongs = it
            }
        }
    }

    fun changeSortBy(sortBy: HistorySortBy) {
        this.sortBy = sortBy
        sortByFlow.value = sortBy
    }

    fun changeSortOrder(sortOrder: SortOrder) {
        this.sortOrder = sortOrder
        sortOrderFlow.value = sortOrder
    }

    fun createPlaylist(name: String, songIds: List<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val playlistId = db.insert(Playlist(name = name))
                if (playlistId != -1L) {
                    val songPlaylistMaps = songIds.mapIndexed { index, songId ->
                        SongPlaylistMap(
                            songId = songId,
                            playlistId = playlistId,
                            position = index
                        )
                    }
                    db.insertSongPlaylistMaps(songPlaylistMaps)
                }
            }
        }
    }

    fun move(from: Int, to: Int) {
        val mutableSongs = historySongs.toMutableList()
        Collections.swap(mutableSongs, from, to)
        historySongs = mutableSongs
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                db.clearEvents()
            }
        }
    }
}
