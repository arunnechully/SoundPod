package com.github.soundpod.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.innertube.Innertube
import com.github.innertube.requests.artistPage
import com.github.soundpod.db
import com.github.soundpod.models.Artist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistViewModel : ViewModel() {
    var artist: Artist? by mutableStateOf(null)
    var artistPage: Innertube.ArtistPage? by mutableStateOf(null)

    fun toggleBookmark() {
        val currentArtist = artist ?: return
        val bookmarkedAt = if (currentArtist.bookmarkedAt == null) System.currentTimeMillis() else null
        val updatedArtist = currentArtist.copy(bookmarkedAt = bookmarkedAt)
        viewModelScope.launch(Dispatchers.IO) {
            db.update(updatedArtist)
        }
    }

    suspend fun loadArtist(browseId: String, tabIndex: Int) {
        db
            .artist(browseId)
            .combine(snapshotFlow { tabIndex }.map { it != 4 }) { artist, mustFetch -> artist to mustFetch }
            .distinctUntilChanged()
            .collect { (currentArtist, mustFetch) ->
                artist = currentArtist

                if (artistPage == null && mustFetch && !browseId.startsWith("local_artist_")) {
                    withContext(Dispatchers.IO) {
                        Innertube.artistPage(browseId = browseId)
                            ?.onSuccess { currentArtistPage ->
                                artistPage = currentArtistPage

                                db.upsert(
                                    Artist(
                                        id = browseId,
                                        name = currentArtistPage.name,
                                        thumbnailUrl = currentArtistPage.thumbnail?.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = currentArtist?.bookmarkedAt
                                    )
                                )
                            }
                    }
                }
            }
    }
}
