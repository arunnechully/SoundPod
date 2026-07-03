package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.soundpod.db
import com.github.soundpod.enums.PlaylistSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.PlaylistPreview

class HomePlaylistsViewModel : ViewModel() {
    var items: List<PlaylistPreview> by mutableStateOf(emptyList())
    var offlineThumbnail: String? by mutableStateOf(null)
    var offlineCount: Int by mutableIntStateOf(0)

    suspend fun loadPlaylists(
        sortBy: PlaylistSortBy,
        sortOrder: SortOrder
    ) {
        db
            .playlistPreviews(sortBy, sortOrder)
            .collect { items = it }
    }
}