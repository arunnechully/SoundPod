package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.innertube.Innertube
import com.github.innertube.requests.artistPage
import com.github.innertube.requests.charts
import com.github.innertube.requests.newReleases
import com.github.innertube.requests.relatedPage
import com.github.soundpod.db
import com.github.soundpod.models.Artist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FollowingViewModel : ViewModel() {
    var followedArtists: List<Artist> by mutableStateOf(emptyList())
    var recommendations: Result<List<Innertube.SongItem>?>? by mutableStateOf(null)
    var latestReleases: List<Innertube.AlbumItem> by mutableStateOf(emptyList())
    var suggestedArtists: List<Innertube.ArtistItem> by mutableStateOf(emptyList())
    var popularArtists: List<Innertube.ArtistItem> by mutableStateOf(emptyList())
    var isRefreshing by mutableStateOf(false)

    private var recommendationsJob: Job? = null

    init {
        viewModelScope.launch {
            db.followedArtists().collectLatest {
                followedArtists = it
                if (it.isNotEmpty()) {
                    loadRemix(it)
                } else {
                    loadPopularArtists()
                    recommendations = null
                    latestReleases = emptyList()
                    suggestedArtists = emptyList()
                }
            }
        }
    }

    private fun loadPopularArtists() {
        viewModelScope.launch(Dispatchers.IO) {
            Innertube.charts()?.onSuccess { songs ->
                // Extract unique artists from popular songs
                popularArtists = songs?.mapNotNull { song ->
                    song.authors?.firstOrNull()?.let { author ->
                        Innertube.ArtistItem(
                            info = author,
                            subscribersCountText = null,
                            thumbnail = song.thumbnail
                        )
                    }
                }?.distinctBy { it.key }?.take(10) ?: emptyList()
            }
        }
    }

    private fun <T : Innertube.Item> interleave(lists: List<List<T>>): List<T> {
        val result = mutableListOf<T>()
        val iterators = lists.map { it.iterator() }
        val seenKeys = mutableSetOf<String>()

        var hasMore = true
        while (hasMore) {
            hasMore = false
            for (iterator in iterators) {
                if (iterator.hasNext()) {
                    val item = iterator.next()
                    if (seenKeys.add(item.key)) {
                        result.add(item)
                    }
                    hasMore = true
                }
            }
        }
        return result
    }

    fun loadRemix(artists: List<Artist> = followedArtists) {
        if (artists.isEmpty()) return
        
        recommendationsJob?.cancel()
        recommendations = null
        isRefreshing = true

        recommendationsJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    // Take up to 5 followed artists to generate remix
                    val seedArtists = artists.shuffled().take(5)
                    
                    val artistPagesDeferreds = seedArtists.map { artist ->
                        async { Innertube.artistPage(browseId = artist.id)?.getOrNull() }
                    }
                    
                    val artistPages = artistPagesDeferreds.mapNotNull { it.await() }

                    // Fetch recommendations (related songs)
                    val relatedSongsDeferreds = artistPages.map { artistPage ->
                        async {
                            val seedSong = artistPage.songs?.firstOrNull() ?: return@async null
                            Innertube.relatedPage(videoId = seedSong.key)?.getOrNull()?.songs
                        }
                    }

                    // Fetch suggested artists (fans also like)
                    suggestedArtists = artistPages.flatMap { it.relatedArtists ?: emptyList() }
                        .distinctBy { it.key }
                        .shuffled()
                        .take(15)

                    // Fetch latest releases
                    latestReleases = artistPages.flatMap { (it.albums ?: emptyList()) + (it.singles ?: emptyList()) }
                        .distinctBy { it.key }
                        .sortedByDescending { it.year }
                        .take(15)

                    val results = relatedSongsDeferreds.mapNotNull { it.await() }
                    
                    if (results.isNotEmpty()) {
                        val interleaved = interleave(results).take(50)
                        recommendations = Result.success(interleaved)
                    } else {
                        recommendations = Result.success(null)
                    }
                    isRefreshing = false
                }
            }
        }
    }
}
