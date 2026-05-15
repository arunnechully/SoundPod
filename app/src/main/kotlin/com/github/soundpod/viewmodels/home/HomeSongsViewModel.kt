package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.soundpod.db
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeSongsViewModel : ViewModel() {
    var items: List<Song> by mutableStateOf(emptyList())
        private set
    private var dbJob: Job? = null

    fun loadSongs(
        sortBy: SongSortBy,
        sortOrder: SortOrder
    ) {
        dbJob?.cancel()

        dbJob = viewModelScope.launch {
            db.songs(sortBy, sortOrder)
                .collect { sortedSongs ->
                    items = sortedSongs
                }
        }
    }
}