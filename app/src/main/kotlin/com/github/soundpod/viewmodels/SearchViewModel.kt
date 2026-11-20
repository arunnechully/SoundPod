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
import com.github.soundcore.SoundcoreSong
import com.github.soundcore.asSoundcoreAlbumItem
import com.github.soundcore.asSoundcoreArtistItem
import com.github.soundcore.asSoundcorePlaylistItem
import com.github.soundcore.asSoundcoreSongItem
import com.github.soundpod.MainApplication
import com.github.soundpod.utils.PlaybackSource
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.playbackSourceKey
import com.github.soundpod.utils.preferences
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
    var soundcoreResults by mutableStateOf<List<SoundcoreSong>?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private var lastSearchedQuery = ""

    private fun getItemName(item: Innertube.Item): String? = when (item) {
        is Innertube.SongItem -> item.info?.name
        is Innertube.AlbumItem -> item.info?.name
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
            soundcoreResults = null
            isLoading = false
            return
        }

        isLoading = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val playbackSource = MainApplication.appContext.preferences.getEnum(playbackSourceKey, PlaybackSource.Automatic)
                if (playbackSource == PlaybackSource.Soundcore) {
                    val soundcoreSongsDeferred = async { Soundcore.searchSongs(query) }
                    val soundcoreAlbumsDeferred = async { Soundcore.searchAlbums(query) }
                    val soundcoreArtistsDeferred = async { Soundcore.searchArtists(query) }
                    val soundcorePlaylistsDeferred = async { Soundcore.searchPlaylists(query) }

                    val soundcoreSongs = soundcoreSongsDeferred.await()
                    val soundcoreAlbums = soundcoreAlbumsDeferred.await()
                    val soundcoreArtists = soundcoreArtistsDeferred.await()
                    val soundcorePlaylists = soundcorePlaylistsDeferred.await()

                    soundcoreResults = soundcoreSongs.take(10)
                    songResults = soundcoreSongs.map { it.asSoundcoreSongItem }
                    albumResults = soundcoreAlbums.map { it.asSoundcoreAlbumItem }
                    artistResults = soundcoreArtists.map { it.asSoundcoreArtistItem }
                    playlistResults = soundcorePlaylists.map { it.asSoundcorePlaylistItem }
                    videoResults = emptyList()
                } else if (playbackSource == PlaybackSource.Automatic) {
                    val songsDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Song.value,
                            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                        )
                    }
                    val albumsDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Album.value,
                            fromMusicShelfRendererContent = Innertube.AlbumItem::from
                        )
                    }
                    val artistsDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Artist.value,
                            fromMusicShelfRendererContent = Innertube.ArtistItem::from
                        )
                    }
                    val videosDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Video.value,
                            fromMusicShelfRendererContent = Innertube.VideoItem::from
                        )
                    }
                    val playlistsDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.CommunityPlaylist.value,
                            fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                        )
                    }
                    val soundcoreSongsDeferred = async { Soundcore.searchSongs(query) }
                    val soundcoreAlbumsDeferred = async { Soundcore.searchAlbums(query) }
                    val soundcoreArtistsDeferred = async { Soundcore.searchArtists(query) }
                    val soundcorePlaylistsDeferred = async { Soundcore.searchPlaylists(query) }

                    val innertubeSongs = songsDeferred.await()?.getOrNull()?.items
                    val innertubeAlbums = albumsDeferred.await()?.getOrNull()?.items
                    val innertubeArtists = artistsDeferred.await()?.getOrNull()?.items
                    val innertubeVideos = videosDeferred.await()?.getOrNull()?.items
                    val innertubePlaylists = playlistsDeferred.await()?.getOrNull()?.items

                    val soundcoreSongs = soundcoreSongsDeferred.await()
                    val soundcoreAlbums = soundcoreAlbumsDeferred.await()
                    val soundcoreArtists = soundcoreArtistsDeferred.await()
                    val soundcorePlaylists = soundcorePlaylistsDeferred.await()

                    soundcoreResults = soundcoreSongs.take(10)
                    songResults = mergeItems(innertubeSongs, soundcoreSongs.map { it.asSoundcoreSongItem }).take(10)
                    albumResults = mergeItems(innertubeAlbums, soundcoreAlbums.map { it.asSoundcoreAlbumItem }).take(10)
                    artistResults = mergeItems(innertubeArtists, soundcoreArtists.map { it.asSoundcoreArtistItem }).take(10)
                    playlistResults = mergeItems(innertubePlaylists, soundcorePlaylists.map { it.asSoundcorePlaylistItem }).take(10)
                    videoResults = innertubeVideos?.take(6)
                } else {
                    val songsDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Song.value,
                            fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                        )
                    }
                    val albumsDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Album.value,
                            fromMusicShelfRendererContent = Innertube.AlbumItem::from
                        )
                    }
                    val artistsDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Artist.value,
                            fromMusicShelfRendererContent = Innertube.ArtistItem::from
                        )
                    }
                    val videosDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.Video.value,
                            fromMusicShelfRendererContent = Innertube.VideoItem::from
                        )
                    }
                    val playlistsDeferred = async {
                        Innertube.searchPage(
                            query = query,
                            params = Innertube.SearchFilter.CommunityPlaylist.value,
                            fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                        )
                    }
                    val soundcoreDeferred = async {
                        Soundcore.searchSongs(query)
                    }

                    songResults = songsDeferred.await()?.getOrNull()?.items?.take(3)
                    albumResults = albumsDeferred.await()?.getOrNull()?.items?.take(8)
                    artistResults = artistsDeferred.await()?.getOrNull()?.items?.take(8)
                    videoResults = videosDeferred.await()?.getOrNull()?.items?.take(6)
                    playlistResults = playlistsDeferred.await()?.getOrNull()?.items?.take(8)
                    soundcoreResults = soundcoreDeferred.await().take(10)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}
