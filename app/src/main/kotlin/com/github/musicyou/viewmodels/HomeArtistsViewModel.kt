package com.github.musicyou.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.musicyou.Database
import com.github.musicyou.enums.ArtistSortBy
import com.github.musicyou.enums.SortOrder
import com.github.musicyou.models.Artist

class HomeArtistsViewModel : ViewModel() {
    var items: List<Artist> by mutableStateOf(emptyList())

    suspend fun loadArtists(
        sortBy: ArtistSortBy,
        sortOrder: SortOrder
    ) {
        Database
            .artists(sortBy, sortOrder)
            .collect { items = it }
    }
}