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
import com.github.innertube.requests.recommendations
import com.github.innertube.requests.searchPage
import com.github.innertube.utils.from
import com.github.soundcore.Soundcore
import com.github.soundcore.SoundcoreRecommender
import com.github.soundcore.SoundcoreSong
import com.github.soundcore.asSoundcoreAlbumItem
import com.github.soundcore.asSoundcoreArtistItem
import com.github.soundcore.asSoundcorePlaylistItem
import com.github.soundcore.asSoundcoreSongItem
import com.github.soundpod.appContext
import com.github.soundpod.db
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.models.Song
import com.github.soundpod.utils.PlaybackSource
import com.github.soundpod.utils.ScreenCache
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.isScreenCacheEnabledKey
import com.github.soundpod.utils.playbackSourceKey
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
    var latestQualifiedPlaybackTimestamp: Long? by mutableStateOf(null)
        private set
    private var job: Job? = null

    companion object {
        private const val CACHE_EXPIRATION = 30 * 60 * 1000L
        private const val PERSISTENT_CACHE_PREFIX = "quick_picks_cache_v5_"
        private const val MIN_HISTORY_PLAY_TIME_MS = 30000L // 30 seconds
    }

    init {
        viewModelScope.launch {
            db.history(limit = 10, minPlayTimeMs = MIN_HISTORY_PLAY_TIME_MS).collect {
                historySongs = it
            }
        }
        viewModelScope.launch {
            db.latestQualifiedPlaybackTimestamp(MIN_HISTORY_PLAY_TIME_MS).collect {
                latestQualifiedPlaybackTimestamp = it
            }
        }
    }

    private fun getSeedSongsFlow(source: QuickPicksSource, limit: Int): Flow<List<Song>> = when (source) {
        QuickPicksSource.Default -> db.trending(limit)
        QuickPicksSource.Custom -> db.randomSongs(limit)
    }

    private fun getCached(key: String): Innertube.RelatedPage? {
        return ScreenCache.load(key)
    }

    private fun saveToCache(key: String, page: Innertube.RelatedPage) {
        ScreenCache.save(key, page)
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
        val playbackSource = appContext.preferences.getEnum(playbackSourceKey, PlaybackSource.Automatic)
        val cacheKey = PERSISTENT_CACHE_PREFIX + quickPicksSource.name + "_" + playbackSource.name
        val cached = if (isScreenCacheEnabled) getCached(cacheKey) else null
        if (cached != null) {
            relatedPageResult = Result.success(cached)
        }

        val cacheTimestamp = ScreenCache.getTimestamp(cacheKey)
        val cacheIncludesPlayback = quickPicksSource != QuickPicksSource.Default ||
            cacheTimestamp >= (latestQualifiedPlaybackTimestamp ?: 0L)
        if (!forceRefresh && cached != null &&
            !ScreenCache.isExpired(cacheKey, CACHE_EXPIRATION) && cacheIncludesPlayback) {
            return
        }

        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            if (playbackSource == PlaybackSource.Soundcore) {
                coroutineScope {
                    val history = db.history(limit = 8, minPlayTimeMs = MIN_HISTORY_PLAY_TIME_MS).first()
                    val historySongIds = history.mapTo(mutableSetOf()) { it.id }

                    val seedSongIdDeferreds = history.map { song ->
                        async {
                            if (!song.id.startsWith("content://") && !song.id.contains("http") && song.id.length != 11) {
                                song.id
                            } else {
                                val query = listOfNotNull(song.title, song.artistsText).joinToString(" ")
                                Soundcore.searchSongs(query).firstOrNull()?.id
                            }
                        }
                    }

                    val followed = db.followedArtists().first()
                    val followedArtistNames = followed.mapNotNull { it.name }
                    val historyArtistNames = history.mapNotNull { it.artistsText?.split(",")?.firstOrNull()?.trim() }
                    val seedArtistNames = (historyArtistNames + followedArtistNames).distinct().take(5)

                    val seedSongIds = seedSongIdDeferreds.mapNotNull { it.await() }

                    val recommendedSongs = SoundcoreRecommender.getRecommendedSongs(
                        seedSongIds = seedSongIds,
                        seedArtistNames = seedArtistNames,
                        excludeSongIds = historySongIds,
                        limit = 40
                    )

                    val launchDataDeferred = async { Soundcore.getLaunchData() }
                    val searchArtistDeferreds = seedArtistNames.take(3).map { name -> async { Soundcore.searchArtists(name) } }

                    val launchData = launchDataDeferred.await()
                    val soundcoreSongs = recommendedSongs.map { it.asSoundcoreSongItem }
                    val soundcoreAlbums = launchData.topAlbums.map { it.asSoundcoreAlbumItem }.take(15)
                    val soundcorePlaylists = launchData.topPlaylists.map { it.asSoundcorePlaylistItem }.take(15)
                    val soundcoreArtists = interleave(searchArtistDeferreds.map { it.await().map { a -> a.asSoundcoreArtistItem } }).take(15)

                    val page = Innertube.RelatedPage(
                        songs = soundcoreSongs.ifEmpty { null },
                        albums = soundcoreAlbums.ifEmpty { null },
                        artists = soundcoreArtists.ifEmpty { null },
                        playlists = soundcorePlaylists.ifEmpty { null }
                    )

                    if (soundcoreSongs.isNotEmpty()) {
                        val res = Result.success(page)
                        if (isScreenCacheEnabled) {
                            saveToCache(cacheKey, page)
                        }
                        relatedPageResult = res
                    } else if (cached != null) {
                        relatedPageResult = Result.success(cached)
                    } else {
                        relatedPageResult = Result.failure(Exception("Failed to load Soundcore QuickPicks"))
                    }
                }
                return@launch
            }

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

                    // Charts are only a fallback when there is no listening or following history.
                    if (seeds.isEmpty()) {
                        Innertube.charts()?.getOrNull()?.take(3)?.forEach { item ->
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
                    async {
                        val videoId = if (song.id.startsWith("content://")) {
                            Innertube.searchPage(
                                query = listOfNotNull(song.title, song.artistsText).joinToString(" "),
                                params = Innertube.SearchFilter.Song.value,
                                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                            )?.getOrNull()?.items?.firstOrNull { it.key.isNotEmpty() }?.key
                        } else {
                            song.id
                        }
                        videoId?.let { Innertube.relatedPage(videoId = it)?.getOrNull() }
                    }
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

                if ((mergedPage == null || mergedPage.songs.isNullOrEmpty()) &&
                    quickPicksSource == QuickPicksSource.Default && seedSongs.isNotEmpty()) {
                    val searchedSongs = seedSongs.take(2).map { song ->
                        async {
                            val query = song.artistsText
                                ?.takeUnless { it.isBlank() || it == "<unknown>" }
                                ?: song.title
                            Innertube.searchPage(
                                query = query,
                                params = Innertube.SearchFilter.Song.value,
                                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                            )?.getOrNull()?.items.orEmpty()
                        }
                    }.map { it.await() }
                    val seedIds = seedSongs.mapTo(mutableSetOf()) { it.id }
                    val songs = interleave(searchedSongs)
                        .filter { it.key.isNotEmpty() && it.key !in seedIds }
                        .take(40)
                    if (songs.isNotEmpty()) mergedPage = Innertube.RelatedPage(songs = songs)
                }

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
                    for (videoId in globalFallbacks) {
                        val fallbackPage = Innertube.relatedPage(videoId = videoId)?.getOrNull()
                        if (!fallbackPage?.songs.isNullOrEmpty()) {
                            mergedPage = fallbackPage
                            break
                        }
                    }
                }

                if (mergedPage == null || mergedPage.songs.isNullOrEmpty()) {
                    val songs = Innertube.recommendations()?.getOrNull()
                    if (!songs.isNullOrEmpty()) {
                        mergedPage = Innertube.RelatedPage(songs = songs)
                    }
                }

                val finalResult = mergedPage?.takeIf { !it.songs.isNullOrEmpty() }?.let { Result.success(it) }
                    ?: Result.failure(Exception("Failed to load Quick Picks"))

                finalResult.getOrNull()?.let {
                    if (isScreenCacheEnabled) {
                        saveToCache(cacheKey, it)
                    }
                }

                relatedPageResult = if (finalResult.isFailure && cached != null) {
                    Result.success(cached)
                } else {
                    finalResult
                }
            }
        }
    }
}
