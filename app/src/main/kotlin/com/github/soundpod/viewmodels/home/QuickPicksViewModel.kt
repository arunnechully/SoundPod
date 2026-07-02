package com.github.soundpod.viewmodels.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.innertube.Innertube
import com.github.innertube.requests.charts
import com.github.innertube.requests.recommendations
import com.github.innertube.requests.relatedPage
import com.github.innertube.requests.searchPage
import com.github.innertube.utils.from
import com.github.soundpod.appContext
import com.github.soundpod.db
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.models.Song
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.quickPicksCustomGenreKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickPicksViewModel : ViewModel() {
    var relatedPageResult: Result<Innertube.RelatedPage?>? by mutableStateOf(null)
    private var job: Job? = null

    private val _preFetchFlow = MutableSharedFlow<List<String>>(replay = 5)
    val preFetchFlow = _preFetchFlow.asSharedFlow()

    @Suppress("SameParameterValue")
    private fun getSeedSongsFlow(source: QuickPicksSource, limit: Int): Flow<List<Song>> = when (source) {
        QuickPicksSource.Default -> db.seedSongs(limit)
        QuickPicksSource.Custom -> db.randomSongs(limit)
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

    fun loadQuickPicks(quickPicksSource: QuickPicksSource) {
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            Log.d("SoundPod", "Loading Quick Picks for source: $quickPicksSource")
            val seedSongs = runCatching {
                when (quickPicksSource) {
                    QuickPicksSource.Custom -> {
                        val customGenre = appContext.preferences.getString(quickPicksCustomGenreKey, "Psaltic music") ?: "Psaltic music"
                        val searchResult = Innertube.searchPage(
                            query = customGenre,
                            params = Innertube.SearchFilter.Song.value,
                            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                        )?.getOrNull()
                        
                        searchResult?.items?.take(3)?.map { item -> 
                            val mediaItem = item.asMediaItem
                            Song(
                                id = mediaItem.mediaId,
                                title = mediaItem.mediaMetadata.title.toString(),
                                artistsText = mediaItem.mediaMetadata.artist.toString(),
                                durationText = null,
                                thumbnailUrl = mediaItem.mediaMetadata.artworkUri.toString()
                            )
                        } ?: emptyList()
                    }
                    QuickPicksSource.Default -> {
                        val seeds = getSeedSongsFlow(quickPicksSource, 3).first()
                        if (seeds.isNotEmpty()) {
                            seeds
                        } else {
                            Innertube.recommendations()?.getOrNull()?.take(3)?.map { item ->
                                val mediaItem = item.asMediaItem
                                Song(
                                    id = mediaItem.mediaId,
                                    title = mediaItem.mediaMetadata.title.toString(),
                                    artistsText = mediaItem.mediaMetadata.artist.toString(),
                                    durationText = null,
                                    thumbnailUrl = mediaItem.mediaMetadata.artworkUri.toString()
                                )
                            } ?: Innertube.charts()?.getOrNull()?.take(3)?.map { item ->
                                val mediaItem = item.asMediaItem
                                Song(
                                    id = mediaItem.mediaId,
                                    title = mediaItem.mediaMetadata.title.toString(),
                                    artistsText = mediaItem.mediaMetadata.artist.toString(),
                                    durationText = null,
                                    thumbnailUrl = mediaItem.mediaMetadata.artworkUri.toString()
                                )
                            } ?: emptyList()
                        }
                    }
                }
            }.getOrElse { e ->
                Log.e("SoundPod", "Failed to load seed songs for $quickPicksSource", e)
                emptyList()
            }

            Log.d("SoundPod", "Seed songs found: ${seedSongs.size}")
            if (seedSongs.isNotEmpty()) {
                _preFetchFlow.emit(seedSongs.map { it.id })
            }

            coroutineScope {
                val chartsDeferred = async {
                    runCatching { Innertube.charts()?.getOrNull() }.getOrNull()
                }

                val relatedDeferreds = seedSongs.map { song ->
                    async {
                        val result = Innertube.relatedPage(videoId = song.id)?.getOrNull()
                        result?.songs?.let { songs ->
                            _preFetchFlow.emit(songs.take(5).mapNotNull { it.info?.endpoint?.videoId })
                        }
                        result
                    }
                }

                val relatedResults = relatedDeferreds.mapNotNull { it.await() }
                Log.d("SoundPod", "Related results found: ${relatedResults.size}")
                
                var mergedPage = if (relatedResults.isNotEmpty()) {
                    Innertube.RelatedPage(
                        songs = interleave(relatedResults.map { it.songs ?: emptyList() }).take(40),
                        playlists = interleave(relatedResults.map { it.playlists ?: emptyList() }).take(15),
                        albums = interleave(relatedResults.map { it.albums ?: emptyList() }).take(15),
                        artists = interleave(relatedResults.map { it.artists ?: emptyList() }).take(15)
                    )
                } else null

                if (mergedPage == null || mergedPage.songs.isNullOrEmpty()) {
                    Log.d("SoundPod", "No related songs, trying charts fallback")
                    chartsDeferred.await()?.shuffled()?.take(2)?.forEach { fallbackSong ->
                        val fallbackResult = Innertube.relatedPage(videoId = fallbackSong.key)?.getOrNull()
                        if (fallbackResult != null && !fallbackResult.songs.isNullOrEmpty()) {
                            mergedPage = fallbackResult
                            return@forEach
                        }
                    }
                }

                if (mergedPage == null || mergedPage.songs.isNullOrEmpty()) {
                    Log.d("SoundPod", "Still no songs, trying global fallbacks")
                    val globalFallbacks = listOf("fJ9rUzIMcZQ", "kJQP7kiw5Fk", "JGwWNGJdvx8")
                    mergedPage = Innertube.relatedPage(videoId = globalFallbacks.random())?.getOrNull()
                }

                val finalResult = mergedPage?.let { Result.success(it) } 
                    ?: Result.failure(Exception("Failed to load Quick Picks"))

                finalResult.getOrNull()?.let { page ->
                    page.songs?.take(10)?.mapNotNull { it.info?.endpoint?.videoId }?.let {
                        _preFetchFlow.emit(it)
                    }
                }

                withContext(Dispatchers.Main) {
                    relatedPageResult = finalResult
                }
            }
        }
    }
}
