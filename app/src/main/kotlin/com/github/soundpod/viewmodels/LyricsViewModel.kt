package com.github.soundpod.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.soundpod.db
import com.github.soundpod.utils.LyricLine
import com.github.soundpod.utils.LyricsData
import com.github.soundpod.utils.LyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LyricsViewModel : ViewModel() {
    private val _lyricsData = MutableStateFlow<LyricsData>(LyricsData.None)
    val lyricsData: StateFlow<LyricsData> = _lyricsData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadLyrics(mediaId: String?) {
        if (mediaId == null) {
            _lyricsData.value = LyricsData.None
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            db.lyrics(mediaId).collectLatest { rawLyrics ->
                val parsedData = withContext(Dispatchers.Default) {
                    LyricsParser.parse(
                        syncedLyrics = rawLyrics?.synced,
                        fixedLyrics = rawLyrics?.fixed
                    )
                }
                _lyricsData.value = parsedData
                _isLoading.value = false
            }
        }
    }

    fun getActiveIndex(position: Long, lines: List<LyricLine>): Int {
        if (lines.isEmpty()) return -1
        val index = lines.binarySearch { it.startMs.compareTo(position) }
        return if (index >= 0) index else (-(index + 2)).coerceAtLeast(0)
    }
}