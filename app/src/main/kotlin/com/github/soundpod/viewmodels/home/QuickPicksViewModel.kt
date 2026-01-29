package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.innertube.Innertube
import com.github.innertube.requests.relatedPage
import com.github.soundpod.db
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.models.Song
import kotlinx.coroutines.flow.distinctUntilChanged

class QuickPicksViewModel : ViewModel() {
    var trending: Song? by mutableStateOf(null)
    var relatedPageResult: Result<Innertube.RelatedPage?>? by mutableStateOf(null)

    suspend fun loadQuickPicks(quickPicksSource: QuickPicksSource) {
        val flow = when (quickPicksSource) {
            QuickPicksSource.Trending -> db.trending()
            QuickPicksSource.LastPlayed -> db.lastPlayed()
            QuickPicksSource.Random -> db.randomSong()
        }

        flow.distinctUntilChanged().collect { song ->
            if (quickPicksSource == QuickPicksSource.Random && song != null && trending != null) return@collect

            if ((song == null && relatedPageResult == null) || trending?.id != song?.id || relatedPageResult?.isSuccess != true) {
                relatedPageResult = Innertube.relatedPage(videoId = (song?.id ?: "fJ9rUzIMcZQ"))
            }

            trending = song
        }
    }
}