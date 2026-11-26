package com.github.soundpod.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.innertube.Innertube
import com.github.innertube.requests.searchPage
import com.github.innertube.utils.from
import com.github.soundcore.Soundcore
import com.github.soundcore.asSoundcoreArtistItem
import com.github.soundcore.asSoundcoreSongItem
import com.github.soundpod.db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Locale

class SearchViewModel : ViewModel() {
    var songResults by mutableStateOf<List<Innertube.SongItem>?>(null)
        private set
    var albumResults by mutableStateOf<List<Innertube.AlbumItem>?>(null)
        private set
    var artistResults by mutableStateOf<List<Innertube.ArtistItem>?>(null)
        private set
    var videoResults by mutableStateOf<List<Innertube.VideoItem>?>(null)
        private set
    var playlistResults by mutableStateOf<List<Innertube.PlaylistItem>?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private var lastSearchedQuery = ""

    private fun getItemName(item: Innertube.Item): String? = when (item) {
        is Innertube.SongItem -> {
            val title = item.info?.name.orEmpty()
            val artist = item.authors?.firstOrNull()?.name.orEmpty()
            "$title - $artist"
        }
        is Innertube.AlbumItem -> {
            val title = item.info?.name.orEmpty()
            val artist = item.authors?.firstOrNull()?.name.orEmpty()
            "$title - $artist"
        }
        is Innertube.ArtistItem -> item.info?.name
        is Innertube.PlaylistItem -> item.info?.name
        is Innertube.VideoItem -> item.info?.name
    }

    private fun <T : Innertube.Item> mergeItems(list1: List<T>?, list2: List<T>?): List<T> {
        if (list1.isNullOrEmpty()) return list2.orEmpty()
        if (list2.isNullOrEmpty()) return list1
        val result = mutableListOf<T>()
        val iter1 = list1.iterator()
        val iter2 = list2.iterator()
        val seenKeys = mutableSetOf<String>()
        val seenNames = mutableSetOf<String>()

        var hasMore = true
        while (hasMore) {
            hasMore = false
            if (iter1.hasNext()) {
                val item = iter1.next()
                val normName = getItemName(item)?.lowercase(Locale.ROOT)?.trim().orEmpty()
                if (seenKeys.add(item.key) && (normName.isBlank() || seenNames.add(normName))) {
                    result.add(item)
                }
                hasMore = true
            }
            if (iter2.hasNext()) {
                val item = iter2.next()
                val normName = getItemName(item)?.lowercase(Locale.ROOT)?.trim().orEmpty()
                if (seenKeys.add(item.key) && (normName.isBlank() || seenNames.add(normName))) {
                    result.add(item)
                }
                hasMore = true
            }
        }
        return result
    }

    fun performSearch(query: String) {
        if (query == lastSearchedQuery && songResults != null) return

        lastSearchedQuery = query

        if (query.isBlank()) {
            songResults = null
            albumResults = null
            artistResults = null
            videoResults = null
            playlistResults = null
            isLoading = false
            return
        }

        isLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val songsDeferred = async {
                    runCatching {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Song.value,
                            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                        )
                    }.getOrNull()
                }
                val albumsDeferred = async {
                    runCatching {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Album.value,
                            fromMusicShelfRendererContent = Innertube.AlbumItem::from
                        )
                    }.getOrNull()
                }
                val artistsDeferred = async {
                    runCatching {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Artist.value,
                            fromMusicShelfRendererContent = Innertube.ArtistItem::from
                        )
                    }.getOrNull()
                }
                val videosDeferred = async {
                    runCatching {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Video.value,
                            fromMusicShelfRendererContent = Innertube.VideoItem::from
                        )
                    }.getOrNull()
                }
                val playlistsDeferred = async {
                    runCatching {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.CommunityPlaylist.value,
                            fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                        )
                    }.getOrNull()
                }
                val soundcoreSongsDeferred = async { runCatching { Soundcore.searchSongs(query) }.getOrDefault(emptyList()) }
                val soundcoreArtistsDeferred = async { runCatching { Soundcore.searchArtists(query) }.getOrDefault(emptyList()) }

                val innertubeSongs = songsDeferred.await()?.getOrNull()?.items
                val innertubeAlbums = albumsDeferred.await()?.getOrNull()?.items
                val innertubeArtists = artistsDeferred.await()?.getOrNull()?.items
                val innertubeVideos = videosDeferred.await()?.getOrNull()?.items
                val innertubePlaylists = playlistsDeferred.await()?.getOrNull()?.items

                val soundcoreSongs = soundcoreSongsDeferred.await()
                val soundcoreArtists = soundcoreArtistsDeferred.await()

                val blacklistedIds = runCatching { db.getBlacklistedSongIds().toSet() }.getOrDefault(emptySet())

                songResults = mergeItems(innertubeSongs, soundcoreSongs.map { it.asSoundcoreSongItem })
                    .filterNot { it.key in blacklistedIds }
                    .take(10)
                albumResults = innertubeAlbums?.take(10)
                artistResults = mergeItems(innertubeArtists, soundcoreArtists.map { it.asSoundcoreArtistItem }).take(10)
                playlistResults = innertubePlaylists?.take(10)
                videoResults = innertubeVideos
                    ?.filterNot { it.key in blacklistedIds }
                    ?.take(6)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}
