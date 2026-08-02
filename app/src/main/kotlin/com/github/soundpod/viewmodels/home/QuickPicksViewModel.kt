package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.innertube.Innertube
import com.github.innertube.requests.artistPage
import com.github.innertube.requests.charts
import com.github.innertube.requests.relatedPage
import com.github.innertube.requests.searchPage
import com.github.innertube.utils.from
import com.github.soundpod.appContext
import com.github.soundpod.db
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.models.Song
import com.github.soundpod.utils.ScreenCache
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.isScreenCacheEnabledKey
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.quickPicksCustomGenreKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class QuickPicksViewModel : ViewModel() {
    var relatedPageResult: Result<Innertube.RelatedPage?>? by mutableStateOf(null)
    var historySongs: List<Song> by mutableStateOf(emptyList())
    private var job: Job? = null

    companion object {
        private const val CACHE_EXPIRATION = 30 * 60 * 1000L
        private const val PERSISTENT_CACHE_PREFIX = "quick_picks_cache_v2_"
        private const val MIN_HISTORY_PLAY_TIME_MS = 30000L // 30 seconds
    }

    init {
        viewModelScope.launch {
            db.history(limit = 10, minPlayTimeMs = MIN_HISTORY_PLAY_TIME_MS).collect {
                historySongs = it
            }
        }
    }

    private fun getSeedSongsFlow(source: QuickPicksSource, limit: Int): Flow<List<Song>> = when (source) {
        QuickPicksSource.Default -> db.trending(limit)
        QuickPicksSource.Custom -> db.randomSongs(limit)
    }

    private fun getCached(source: QuickPicksSource): Innertube.RelatedPage? {
        return ScreenCache.load(PERSISTENT_CACHE_PREFIX + source.name)
    }

    private fun saveToCache(source: QuickPicksSource, page: Innertube.RelatedPage) {
        ScreenCache.save(PERSISTENT_CACHE_PREFIX + source.name, page)
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

    fun loadQuickPicks(quickPicksSource: QuickPicksSource, forceRefresh: Boolean = false) {
        val isScreenCacheEnabled = appContext.preferences.getBoolean(isScreenCacheEnabledKey, true)
        val cached = if (isScreenCacheEnabled) getCached(quickPicksSource) else null
        if (cached != null) {
            relatedPageResult = Result.success(cached)
        }

        if (!forceRefresh && cached != null && !ScreenCache.isExpired(PERSISTENT_CACHE_PREFIX + quickPicksSource.name, CACHE_EXPIRATION)) {
            return
        }

        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            val seedSongs = when (quickPicksSource) {
                QuickPicksSource.Custom -> {
                    val customGenre = appContext.preferences.getString(quickPicksCustomGenreKey, "ROCK") ?: "ROCK"
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
                    val seeds = mutableListOf<Song>()

                    // 1. Add seeds from History
                    seeds.addAll(db.history(limit = 2, minPlayTimeMs = MIN_HISTORY_PLAY_TIME_MS).first())

                    // 2. Add seeds from Following
                    val followed = db.followedArtists().first()
                    if (followed.isNotEmpty()) {
                        followed.shuffled().take(2).forEach { artist ->
                            Innertube.artistPage(browseId = artist.id)?.getOrNull()?.songs?.firstOrNull()?.let { item ->
                                val mediaItem = item.asMediaItem
                                seeds.add(
                                    Song(
                                        id = mediaItem.mediaId,
                                        title = mediaItem.mediaMetadata.title.toString(),
                                        artistsText = mediaItem.mediaMetadata.artist.toString(),
                                        durationText = null,
                                        thumbnailUrl = mediaItem.mediaMetadata.artworkUri.toString()
                                    )
                                )
                            }
                        }
                    }

                    // 3. Add seeds from Charts if we don't have enough
                    if (seeds.size < 3) {
                        Innertube.charts()?.getOrNull()?.take(3 - seeds.size)?.forEach { item ->
                            val mediaItem = item.asMediaItem
                            seeds.add(
                                Song(
                                    id = mediaItem.mediaId,
                                    title = mediaItem.mediaMetadata.title.toString(),
                                    artistsText = mediaItem.mediaMetadata.artist.toString(),
                                    durationText = null,
                                    thumbnailUrl = mediaItem.mediaMetadata.artworkUri.toString()
                                )
                            )
                        }
                    }
                    
                    if (seeds.isEmpty()) {
                        seeds.addAll(getSeedSongsFlow(quickPicksSource, 3).first())
                    }
                    
                    seeds.distinctBy { it.id }
                }
            }

            coroutineScope {
                val chartsDeferred = async {
                    runCatching { Innertube.charts()?.getOrNull() }.getOrNull()
                }

                val relatedDeferreds = seedSongs.map { song ->
                    async { Innertube.relatedPage(videoId = song.id)?.getOrNull() }
                }

                val relatedResults = relatedDeferreds.mapNotNull { it.await() }
                
                var mergedPage = if (relatedResults.isNotEmpty()) {
                    Innertube.RelatedPage(
                        songs = interleave(relatedResults.map { it.songs ?: emptyList() }).take(40),
                        playlists = interleave(relatedResults.map { it.playlists ?: emptyList() }).take(15),
                        albums = interleave(relatedResults.map { it.albums ?: emptyList() }).take(15),
                        artists = interleave(relatedResults.map { it.artists ?: emptyList() }).take(15)
                    )
                } else null

                if (mergedPage == null || mergedPage.songs.isNullOrEmpty()) {
                    chartsDeferred.await()?.shuffled()?.take(2)?.forEach { fallbackSong ->
                        val fallbackResult = Innertube.relatedPage(videoId = fallbackSong.key)?.getOrNull()
                        if (fallbackResult != null && !fallbackResult.songs.isNullOrEmpty()) {
                            mergedPage = fallbackResult
                            return@forEach
                        }
                    }
                }

                if (mergedPage == null || mergedPage.songs.isNullOrEmpty()) {
                    val globalFallbacks = listOf("fJ9rUzIMcZQ", "kJQP7kiw5Fk", "JGwWNGJdvx8")
                    mergedPage = Innertube.relatedPage(videoId = globalFallbacks.random())?.getOrNull()
                }

                val finalResult = mergedPage?.let { Result.success(it) } 
                    ?: Result.failure(Exception("Failed to load Quick Picks"))

                finalResult.getOrNull()?.let {
                    if (isScreenCacheEnabled) {
                        saveToCache(quickPicksSource, it)
                    }
                }

                relatedPageResult = finalResult
            }
        }
    }
}
