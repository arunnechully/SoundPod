package com.github.soundpod.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.innertube.Innertube
import com.github.innertube.requests.searchPage
import com.github.innertube.utils.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    var searchResults by mutableStateOf<List<Innertube.SongItem>?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set
        
    private var lastSearchedQuery = ""

    fun performSearch(query: String) {
        if (query == lastSearchedQuery && searchResults != null) return 
        
        lastSearchedQuery = query

        if (query.isBlank()) {
            searchResults = null
            isLoading = false
            return
        }

        isLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = Innertube.searchPage(
                    query = query,
                    params = Innertube.SearchFilter.Song.value,
                    fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                )
                searchResults = result?.getOrNull()?.items?.take(5)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}