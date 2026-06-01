package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.innertube.Innertube
import com.github.innertube.requests.charts
import com.github.innertube.requests.relatedPage
import com.github.soundpod.db
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class QuickPicksViewModel : ViewModel() {
    var trending: Song? by mutableStateOf(null)
    var relatedPageResult: Result<Innertube.RelatedPage?>? by mutableStateOf(null)
    private var job: Job? = null

    companion object {
        private val cache = ConcurrentHashMap<QuickPicksSource, Pair<Innertube.RelatedPage, Long>>()
        private const val CACHE_EXPIRATION = 15 * 60 * 1000L
    }

    fun loadQuickPicks(quickPicksSource: QuickPicksSource, forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            val cached = cache[quickPicksSource]
            if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_EXPIRATION) {
                relatedPageResult = Result.success(cached.first)
                viewModelScope.launch {
                    val song = when (quickPicksSource) {
                        QuickPicksSource.Trending -> db.trending()
                        QuickPicksSource.LastPlayed -> db.lastPlayed()
                        QuickPicksSource.Random -> db.randomSong()
                    }.firstOrNull()
                    trending = song
                }
                return
            }
        }

        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            val flow: Flow<Song?> = when (quickPicksSource) {
                QuickPicksSource.Trending -> db.trending()
                QuickPicksSource.LastPlayed -> db.lastPlayed()
                QuickPicksSource.Random -> db.randomSong()
            }

            val song = flow.distinctUntilChanged().firstOrNull()
            trending = song

            coroutineScope {
                val chartsDeferred = async { 
                    runCatching { Innertube.charts()?.getOrNull() }.getOrNull() 
                }
                
                val videoId = song?.id ?: chartsDeferred.await()?.randomOrNull()?.key
                
                var result = if (videoId != null) {
                    Innertube.relatedPage(videoId = videoId)
                } else null

                if (result?.getOrNull() == null) {
                    val charts = chartsDeferred.await()
                    if (charts != null) {
                        for (fallbackSong in charts.shuffled().take(2)) {
                            val fallbackResult = Innertube.relatedPage(videoId = fallbackSong.key)
                            if (fallbackResult?.getOrNull() != null) {
                                result = fallbackResult
                                break
                            }
                        }
                    }
                }

                if (result?.getOrNull() == null) {
                    val globalFallbacks = listOf("fJ9rUzIMcZQ", "kJQP7kiw5Fk", "JGwWNGJdvx8")
                    result = Innertube.relatedPage(videoId = globalFallbacks.random())
                }

                val finalResult = result ?: Result.failure(Exception("Failed to load Quick Picks"))
                
                finalResult.getOrNull()?.let {
                    cache[quickPicksSource] = it to System.currentTimeMillis()
                }
                
                relatedPageResult = finalResult
            }
        }
    }
}
