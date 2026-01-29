package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.soundpod.db
import com.github.soundpod.enums.ArtistSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.Artist

class HomeArtistsViewModel : ViewModel() {
    var items: List<Artist> by mutableStateOf(emptyList())

    suspend fun loadArtists(
        sortBy: ArtistSortBy,
        sortOrder: SortOrder
    ) {
        db
            .artists(sortBy, sortOrder)
            .collect { items = it }
    }
}