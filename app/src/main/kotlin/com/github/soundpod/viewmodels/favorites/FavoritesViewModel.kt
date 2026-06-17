package com.github.soundpod.viewmodels.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.soundpod.db
import com.github.soundpod.models.Album
import com.github.soundpod.models.Artist
import com.github.soundpod.models.PlaylistPreview
import com.github.soundpod.models.Song
import com.github.soundpod.R
import com.github.soundpod.enums.AlbumSortBy
import com.github.soundpod.enums.ArtistSortBy
import com.github.soundpod.enums.SortOrder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritesViewModel : ViewModel() {
    val tabs = listOf(
        R.string.songs,
        R.string.albums,
        R.string.artists,
        R.string.playlists
    )

    var favoriteSongs: List<Song> by mutableStateOf(emptyList())
        private set
    var favoriteAlbums: List<Album> by mutableStateOf(emptyList())
        private set
    var favoriteArtists: List<Artist> by mutableStateOf(emptyList())
        private set
    var favoritePlaylists: List<PlaylistPreview> by mutableStateOf(emptyList())
        private set

    init {
        viewModelScope.launch {
            db.favorites().collectLatest {
                favoriteSongs = it
            }
        }
        viewModelScope.launch {
            db.albums(AlbumSortBy.DateAdded, SortOrder.Descending).collectLatest {
                favoriteAlbums = it
            }
        }
        viewModelScope.launch {
            db.artists(ArtistSortBy.DateAdded, SortOrder.Descending).collectLatest {
                favoriteArtists = it
            }
        }
        // For now, let's just show all local playlists if we don't have a "favorite" flag yet
        viewModelScope.launch {
            db.playlistPreviewsByNameAsc().collectLatest { previews ->
                favoritePlaylists = previews
            }
        }
    }
}
