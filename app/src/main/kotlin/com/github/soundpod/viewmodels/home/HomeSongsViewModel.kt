package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.soundpod.appContext
import com.github.soundpod.db
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.Song
import com.github.soundpod.query
import com.github.soundpod.utils.queryMediaStoreSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeSongsViewModel : ViewModel() {
    var items: List<Song> by mutableStateOf(emptyList())
        private set
    private var dbJob: Job? = null

    init {
        refreshMediaStore()
    }

    fun refreshMediaStore() {
        viewModelScope.launch(Dispatchers.IO) {
            val localSongs = appContext.queryMediaStoreSongs()
            if (localSongs.isNotEmpty()) {
                query {
                    localSongs.forEach { db.insert(it) }
                }
            }
        }
    }

    fun loadSongs(
        sortBy: SongSortBy,
        sortOrder: SortOrder
    ) {
        dbJob?.cancel()

        dbJob = viewModelScope.launch {
            db.localSongs(sortBy, sortOrder)
                .collect { sortedSongs ->
                    items = sortedSongs
                }
        }
    }
}
