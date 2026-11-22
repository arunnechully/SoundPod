package com.github.soundpod.utils

import androidx.media3.common.MediaItem
import com.github.innertube.Innertube
import com.github.innertube.requests.nextPage
import com.github.innertube.requests.searchPage
import com.github.innertube.utils.from
import com.github.soundcore.Soundcore
import com.github.soundcore.asSoundcoreSongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class YouTubeRadio(
    private var videoId: String? = null,
    private var playlistId: String? = null,
    private var playlistSetVideoId: String? = null,
    private var parameters: String? = null,
    private val title: String? = null,
    private val artist: String? = null
) {
    private var nextContinuation: String? = null

    suspend fun process(): List<MediaItem> {
        var mediaItems: List<MediaItem>? = null

        nextContinuation = withContext(Dispatchers.IO) {
            val continuation = nextContinuation

            if (continuation == null) {
                val isYouTubeId = videoId?.length == 11 && !videoId.orEmpty().startsWith("http")
                var targetVideoId = if (isYouTubeId) videoId else null

                if (targetVideoId == null) {
                    targetVideoId = resolveYouTubeVideoId(videoId, title, artist)
                    if (targetVideoId != null) {
                        videoId = targetVideoId
                    }
                }

                var nextResult = targetVideoId?.let { vid ->
                    Innertube.nextPage(
                        videoId = vid,
                        playlistId = playlistId,
                        params = parameters,
                        playlistSetVideoId = playlistSetVideoId
                    )
                }

                if ((nextResult == null || nextResult.isFailure) && isYouTubeId) {
                    val resolved = resolveYouTubeVideoId(videoId, title, artist)
                    if (resolved != null && resolved != targetVideoId) {
                        videoId = resolved
                        nextResult = Innertube.nextPage(
                            videoId = resolved,
                            playlistId = playlistId,
                            params = parameters,
                            playlistSetVideoId = playlistSetVideoId
                        )
                    }
                }

                val page = nextResult?.map { res ->
                    playlistId = res.playlistId
                    parameters = res.params
                    playlistSetVideoId = res.playlistSetVideoId

                    res.itemsPage
                }?.getOrNull()

                if (page != null) {
                    mediaItems = page.items?.map(Innertube.SongItem::asMediaItem)
                    page.continuation?.takeUnless { nextContinuation == it }
                } else {
                    val currentVid = videoId
                    if (!currentVid.isNullOrBlank()) {
                        val soundcoreRecs = runCatching { Soundcore.getRecommendations(currentVid) }.getOrDefault(emptyList())
                        if (soundcoreRecs.isNotEmpty()) {
                            mediaItems = soundcoreRecs.map { it.asSoundcoreSongItem.asMediaItem }
                        }
                    }
                    null
                }
            } else {
                Innertube.nextPage(continuation = continuation)?.getOrNull()?.let { songsPage ->
                    mediaItems = songsPage.items?.map(Innertube.SongItem::asMediaItem)
                    songsPage.continuation?.takeUnless { nextContinuation == it }
                }
            }
        }

        return mediaItems ?: emptyList()
    }

    private suspend fun resolveYouTubeVideoId(songId: String?, songTitle: String?, songArtist: String?): String? {
        var t = songTitle
        var a = songArtist

        if (t.isNullOrBlank() && !songId.isNullOrBlank()) {
            val details = runCatching { Soundcore.getSongDetails(songId) }.getOrNull()
            t = details?.title
            a = details?.artistsText
        }

        val query = listOfNotNull(t, a).filter { it.isNotBlank() }.joinToString(" ").trim()
        if (query.isBlank()) return null

        return runCatching {
            val searchResult = Innertube.searchPage(
                query = query,
                params = Innertube.SearchFilter.Song.value,
                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
            )?.getOrNull()
            searchResult?.items?.firstOrNull()?.info?.endpoint?.videoId
        }.getOrNull()
    }
}
