package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.soundpod.Database
import com.github.soundpod.enums.PlaylistSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.PlaylistPreview

class HomePlaylistsViewModel : ViewModel() {
    var items: List<PlaylistPreview> by mutableStateOf(emptyList())

    suspend fun loadArtists(
        sortBy: PlaylistSortBy,
        sortOrder: SortOrder
    ) {
        Database
            .playlistPreviews(sortBy, sortOrder)
            .collect { items = it }
    }
}