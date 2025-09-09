package com.github.innertube.requests

import io.ktor.http.Url
import com.github.innertube.Innertube
import com.github.innertube.models.NavigationEndpoint

suspend fun Innertube.albumPage(browseId: String): Result<Innertube.PlaylistOrAlbumPage>? {
    return playlistPage(browseId = browseId)?.map { album ->
        album.url?.let { Url(it).parameters["list"] }?.let { playlistId ->
            playlistPage(browseId = "VL$playlistId")?.getOrNull()?.let { playlist ->
                album.copy(songsPage = playlist.songsPage)
            }
        } ?: album
    }?.map { album ->
        val albumInfo = Innertube.Info(
            name = album.title,
            endpoint = NavigationEndpoint.Endpoint.Browse(browseId = browseId)
        )

        album.copy(
            songsPage = album.songsPage?.copy(
                items = album.songsPage.items?.map { song ->
                    song.copy(
                        authors = song.authors ?: album.authors,
                        album = albumInfo,
                        thumbnail = album.thumbnail
                    )
                }
            )
        )
    }
}