package com.github.soundpod.viewmodels.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import com.github.soundpod.db
import com.github.soundpod.enums.PlaylistSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.PlaylistPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomePlaylistsViewModel : ViewModel() {
    var items: List<PlaylistPreview> by mutableStateOf(emptyList())
    var offlineThumbnail: String? by mutableStateOf(null)
    var offlineCount: Int by mutableIntStateOf(0)

    @UnstableApi
    fun observeOfflineSongs(cache: Cache?) {
        viewModelScope.launch {
            db.songsWithContentLength()
                .map { songsWithLength ->
                    songsWithLength
                        .filterNot {
                            it.song.id.startsWith("content://") || it.song.id.startsWith("file://")
                        }.filter { item ->
                            val length = item.contentLength
                            if (length != null) {
                                cache?.isCached(item.song.id, 0, length) == true
                            } else {
                                (cache?.getCachedBytes(item.song.id, 0, -1) ?: 0L) > 0L
                            }
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