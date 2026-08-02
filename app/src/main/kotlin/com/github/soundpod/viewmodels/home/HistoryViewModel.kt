package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.soundpod.db
import com.github.soundpod.models.Song
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    var historySongs: List<Song> by mutableStateOf(emptyList())
        private set

    companion object {
        private const val MIN_HISTORY_PLAY_TIME_MS = 30000L // 30 seconds
    }

    init {
        viewModelScope.launch {
            db.history(limit = 100, minPlayTimeMs = MIN_HISTORY_PLAY_TIME_MS).collect {
                historySongs = it
            }
        }
    }
}
