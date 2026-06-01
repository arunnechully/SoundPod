package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.innertube.Innertube
import com.github.innertube.requests.charts
import com.github.innertube.requests.relatedPage
import com.github.soundpod.appContext
import com.github.soundpod.db
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.models.Song
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuickPicksViewModel : ViewModel() {
    var relatedPageResult: Result<Innertube.RelatedPage?>? by mutableStateOf(null)
    private var job: Job? = null

    companion object {
        private const val CACHE_EXPIRATION = 30 * 60 * 1000L // 30 minutes
        private const val PERSISTENT_CACHE_PREFIX = "quick_picks_cache_v2_"
        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
    }

    private fun getSeedSongsFlow(source: QuickPicksSource, limit: Int): Flow<List<Song>> = when (source) {
        QuickPicksSource.Trending -> db.trending(limit)
        QuickPicksSource.LastPlayed -> db.lastPlayed(limit)
        QuickPicksSource.Random -> db.randomSongs(limit)
    }

    private fun getCached(source: QuickPicksSource): Innertube.RelatedPage? {
        val cachedJson = appContext.preferences.getString(PERSISTENT_CACHE_PREFIX + source.name, null) ?: return null
        val timestamp = appContext.preferences.getLong(PERSISTENT_CACHE_PREFIX + source.name + "_time", 0)
        
        if (System.currentTimeMillis() - timestamp > CACHE_EXPIRATION) return null
        
        return try {
            json.decodeFromString<Innertube.RelatedPage>(cachedJson)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveToCache(source: QuickPicksSource, page: Innertube.RelatedPage) {
        try {
            val cachedJson = json.encodeToString(page)
            appContext.preferences.edit {
                putString(PERSISTENT_CACHE_PREFIX + source.name, cachedJson)
                putLong(PERSISTENT_CACHE_PREFIX + source.name + "_time", System.currentTimeMillis())
            }
        } catch (_: Exception) {
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

    fun loadQuickPicks(quickPicksSource: QuickPicksSource, forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            val cached = getCached(quickPicksSource)
            if (cached != null) {
                relatedPageResult = Result.success(cached)
                return
            }
        }

        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            val seedSongs = getSeedSongsFlow(quickPicksSource, 3).first()

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
                    saveToCache(quickPicksSource, it)
                }

                relatedPageResult = finalResult
            }
        }
    }
}
