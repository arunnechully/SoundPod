package com.soundpod.music.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundpod.music.data.MusicRepository
import com.soundpod.music.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SongViewModel(application: Application) : AndroidViewModel(application) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _songs.value = MusicRepository.getAllSongs(getApplication())
            _isLoading.value = false
        }
    }
}
