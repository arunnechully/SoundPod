package com.github.soundpod.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.innertube.Innertube
import com.github.innertube.requests.albumPage
import com.github.soundpod.db
import com.github.soundpod.models.Album
import com.github.soundpod.models.SongAlbumMap
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumUiState(
    val album: Album? = null,
    val albumPage: Innertube.PlaylistOrAlbumPage? = null,
    val isLoved: Boolean = false,
    val isLoading: Boolean = true
)

class AlbumViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    private var currentBrowseId: String? = null

    fun initAlbum(browseId: String) {
        if (currentBrowseId == browseId) return
        currentBrowseId = browseId

        viewModelScope.launch {
            db.album(browseId).collect { localAlbum ->
                _uiState.update { 
                    it.copy(
                        album = localAlbum,
                        isLoved = localAlbum?.bookmarkedAt != null,
                        isLoading = localAlbum?.timestamp == null 
                    ) 
                }

                if (localAlbum?.timestamp == null) {
                    fetchAlbumData(browseId)
                }
            }
        }
    }

    fun onTabSelected(tabIndex: Int) {
        val browseId = currentBrowseId ?: return
        val currentState = _uiState.value

        if (currentState.albumPage == null && tabIndex >= 1) {
            fetchAlbumData(browseId)
        }
    }

    private fun fetchAlbumData(browseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Innertube.albumPage(browseId = browseId)
                ?.completed()
                ?.onSuccess { currentAlbumPage ->

                    _uiState.update { it.copy(albumPage = currentAlbumPage, isLoading = false) }

                    db.clearAlbum(browseId)
                    
                    val currentAlbum = _uiState.value.album
                    db.upsert(
                        Album(
                            id = browseId,
                            title = currentAlbumPage.title,
                            thumbnailUrl = currentAlbumPage.thumbnail?.url,
                            year = currentAlbumPage.year,
                            authorsText = currentAlbumPage.authors?.joinToString("") { it.name ?: "" },
                            shareUrl = currentAlbumPage.url,
                            timestamp = System.currentTimeMillis(),
                            bookmarkedAt = currentAlbum?.bookmarkedAt
                        ),
                        currentAlbumPage.songsPage?.items
                            ?.map(Innertube.SongItem::asMediaItem)
                            ?.onEach(db::insert)
                            ?.mapIndexed { position, mediaItem ->
                                SongAlbumMap(
                                    songId = mediaItem.mediaId,
                                    albumId = browseId,
                                    position = position
                                )
                            } ?: emptyList()
                    )
                }
        }
    }

    fun toggleLove() {
        val currentAlbum = _uiState.value.album ?: return

        val isCurrentlyLoved = currentAlbum.bookmarkedAt != null
        val newBookmarkedAt = if (isCurrentlyLoved) null else System.currentTimeMillis()

        _uiState.update { state ->
            state.copy(
                isLoved = !isCurrentlyLoved,
                album = currentAlbum.copy(bookmarkedAt = newBookmarkedAt)
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            db.update(currentAlbum.copy(bookmarkedAt = newBookmarkedAt))
        }
    }
}