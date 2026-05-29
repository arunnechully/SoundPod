package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.innertube.Innertube
import com.github.innertube.requests.charts
import com.github.innertube.requests.relatedPage
import com.github.soundpod.db
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.models.Song
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow

class QuickPicksViewModel : ViewModel() {
    var trending: Song? by mutableStateOf(null)
    var relatedPageResult: Result<Innertube.RelatedPage?>? by mutableStateOf(null)
    private var job: Job? = null

    fun loadQuickPicks(quickPicksSource: QuickPicksSource) {
        job?.cancel()
        job = viewModelScope.launch {
            val flow: Flow<Song?> = when (quickPicksSource) {
                QuickPicksSource.Trending -> db.trending()
                QuickPicksSource.LastPlayed -> db.lastPlayed()
                QuickPicksSource.Random -> db.randomSong()
            }

            flow.distinctUntilChanged().collect { song ->
                if (quickPicksSource == QuickPicksSource.Random && song != null && trending != null) return@collect

                if ((song == null && relatedPageResult == null) || trending?.id != song?.id || relatedPageResult?.isSuccess != true) {
                    coroutineScope {
                        val chartsDeferred = if (song == null) async { Innertube.charts()?.getOrNull() } else null
                        
                        val charts = try { chartsDeferred?.await() } catch (e: Exception) { null }
                        val videoId = song?.id ?: charts?.randomOrNull()?.key
                        
                        var result = if (videoId != null) Innertube.relatedPage(videoId = videoId) else null
                        
                        if (result?.getOrNull() == null && song == null) {
                            val fallbackSongs = charts ?: emptyList()
                            for (fallbackSong in fallbackSongs.shuffled().take(3)) {
                                if (fallbackSong.key == videoId) continue
                                val fallbackResult = Innertube.relatedPage(videoId = fallbackSong.key)
                                if (fallbackResult?.getOrNull() != null) {
                                    result = fallbackResult
                                    break
                                }
                            }
                        }

                        if (result?.getOrNull() == null) {
                            val globalFallbacks = listOf("fJ9rUzIMcZQ", "kJQP7kiw5Fk", "JGwWNGJdvx8", "OPf0YbXqDm0")
                            val fallbackResult = Innertube.relatedPage(videoId = globalFallbacks.random())
                            relatedPageResult = fallbackResult ?: Result.failure(Exception("Failed to load Quick Picks"))
                        } else {
                            relatedPageResult = result
                        }
                    }
                }

                trending = song
            }
        }
    }
}
