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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SearchViewModel : ViewModel() {
    var songResults by mutableStateOf<List<Innertube.SongItem>?>(null)
        private set
    var albumResults by mutableStateOf<List<Innertube.AlbumItem>?>(null)
        private set
    var artistResults by mutableStateOf<List<Innertube.ArtistItem>?>(null)
        private set
    var playlistResults by mutableStateOf<List<Innertube.PlaylistItem>?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val _preFetchFlow = MutableSharedFlow<List<String>>(replay = 1)
    val preFetchFlow = _preFetchFlow.asSharedFlow()
        
    private var lastSearchedQuery = ""

    fun performSearch(query: String) {
        if (query == lastSearchedQuery && songResults != null) return 
        
        lastSearchedQuery = query

        if (query.isBlank()) {
            songResults = null
            albumResults = null
            artistResults = null
            playlistResults = null
            isLoading = false
            return
        }

        isLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songsDeferred = async {
                    Innertube.searchPage(
                        query = query,
                        params = Innertube.SearchFilter.Song.value,
                        fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                    )
                }
                val albumsDeferred = async {
                    Innertube.searchPage(
                        query = query,
                        params = Innertube.SearchFilter.Album.value,
                        fromMusicShelfRendererContent = Innertube.AlbumItem::from
                    )
                }
                val artistsDeferred = async {
                    Innertube.searchPage(
                        query = query,
                        params = Innertube.SearchFilter.Artist.value,
                        fromMusicShelfRendererContent = Innertube.ArtistItem::from
                    )
                }
                val playlistsDeferred = async {
                    Innertube.searchPage(
                        query = query,
                        params = Innertube.SearchFilter.CommunityPlaylist.value,
                        fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                    )
                }

                val songs = songsDeferred.await()?.getOrNull()?.items
                songResults = songs?.take(10)
                albumResults = albumsDeferred.await()?.getOrNull()?.items?.take(8)
                artistResults = artistsDeferred.await()?.getOrNull()?.items?.take(8)
                playlistResults = playlistsDeferred.await()?.getOrNull()?.items?.take(8)

                songs?.take(5)?.map { it.key }?.let {
                    _preFetchFlow.emit(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}
