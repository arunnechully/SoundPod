package com.github.soundpod.viewmodels.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

    var sortBy by mutableStateOf(SongSortBy.DateAdded)
    var sortOrder by mutableStateOf(SortOrder.Descending)

    init {
        viewModelScope.launch {
            combine(
                db.favorites(),
                snapshotFlow { sortBy },
                snapshotFlow { sortOrder }
            ) { favorites, sortBy, sortOrder ->
                val sortedList = when (sortBy) {
                    SongSortBy.Title -> favorites.sortedBy { it.title.lowercase() }
                    SongSortBy.Artist -> favorites.sortedBy { it.artistsText?.lowercase() ?: "" }
                    SongSortBy.PlayTime -> favorites.sortedBy { it.totalPlayTimeMs }
                    SongSortBy.DateAdded -> favorites // db.favorites() is already sorted by likedAt (DateAdded)
                }
                if (sortOrder == SortOrder.Descending) sortedList.reversed() else sortedList
            }.collectLatest {
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
