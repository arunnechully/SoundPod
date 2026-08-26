package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import com.github.soundpod.db
import com.github.soundpod.enums.PlaylistSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.PlaylistPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomePlaylistsViewModel : ViewModel() {
    var items: List<PlaylistPreview> by mutableStateOf(emptyList())
    var offlineThumbnail: String? by mutableStateOf(null)
    var offlineCount: Int by mutableIntStateOf(0)

    private var observationJob: Job? = null

    @UnstableApi
    fun observeOfflineSongs(cache: Cache?, cacheChanges: kotlinx.coroutines.flow.Flow<Unit>) {
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            combine(
                db.downloadedSongs(),
                cacheChanges.onStart { emit(Unit) }
            ) { downloadedSongs, _ ->
                downloadedSongs
                    .filter { item ->
                        val metadata = cache?.getContentMetadata(item.song.id)
                        val length = metadata?.let { ContentMetadata.getContentLength(it) }?.takeIf { it > 0 }
                            ?: item.contentLength ?: 0L

                        length > 0 && cache?.isCached(item.song.id, 0, length) == true
                    }
            }
                .flowOn(Dispatchers.IO)
                .collect { cachedSongs ->
                    offlineCount = cachedSongs.size
                    offlineThumbnail = cachedSongs.shuffled().firstOrNull()?.song?.thumbnailUrl
                }
        }
    }

    suspend fun loadPlaylists(
        sortBy: PlaylistSortBy,
        sortOrder: SortOrder
    ) {
        db
            .playlistPreviews(sortBy, sortOrder)
            .collect { items = it }
    }
}