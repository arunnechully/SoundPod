package com.github.soundpod.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import com.github.innertube.Innertube
import com.github.innertube.requests.artistPage
import com.github.soundpod.db
import com.github.soundpod.models.Artist
import com.github.soundpod.utils.ScreenCache
import com.github.soundpod.utils.isScreenCacheEnabledKey
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ArtistViewModel : ViewModel() {
    var artist: Artist? by mutableStateOf(null)
    var artistPage: Innertube.ArtistPage? by mutableStateOf(null)

    companion object {
        private const val CACHE_EXPIRATION = 60 * 60 * 1000L // 1 hour
    }

    suspend fun loadArtist(browseId: String, tabIndex: Int) {
        val context = com.github.soundpod.appContext
        val isScreenCacheEnabled = context.preferences.getBoolean(isScreenCacheEnabledKey, true)
        val cacheKey = "artist_$browseId"

        if (artistPage == null && isScreenCacheEnabled) {
            artistPage = ScreenCache.load(cacheKey)
        }

        db
            .artist(browseId)
            .combine(snapshotFlow { tabIndex }.map { it != 4 }) { artist, mustFetch -> artist to mustFetch }
            .distinctUntilChanged()
            .collect { (currentArtist, mustFetch) ->
                artist = currentArtist

                val isExpired = ScreenCache.isExpired(cacheKey, CACHE_EXPIRATION)

                if (artistPage == null || (isExpired && mustFetch)) {
                    withContext(Dispatchers.IO) {
                        Innertube.artistPage(browseId = browseId)
                            ?.onSuccess { currentArtistPage ->
                                artistPage = currentArtistPage
                                if (isScreenCacheEnabled) {
                                    ScreenCache.save(cacheKey, currentArtistPage)
                                }

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
