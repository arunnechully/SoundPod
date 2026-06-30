package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.BrowseResponse
import com.github.innertube.models.MusicCarouselShelfRenderer
import com.github.innertube.models.MusicShelfRenderer
import com.github.innertube.models.SectionListRenderer
import com.github.innertube.models.bodies.BrowseBody
import com.github.innertube.utils.findSectionByTitle
import com.github.innertube.utils.from
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.artistPage(browseId: String): Result<Innertube.ArtistPage>? =
    runCatchingNonCancellable {
        val response = client.post(BROWSE) {
            setBody(
                BrowseBody(
                    localized = false,
                    browseId = browseId
                )
            )
            mask("contents,header")
        }.body<BrowseResponse>()

        fun findSectionByTitle(text: String): SectionListRenderer.Content? {
            return response
                .contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.get(0)
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.findSectionByTitle(text)
        }

        val artistName = response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.title
            ?.text

        val songsSection = findSectionByTitle("Top songs")?.musicShelfRenderer
        val albumsSection = findSectionByTitle("Albums")?.musicCarouselShelfRenderer
        val singlesSection = findSectionByTitle("Singles & EPs")?.musicCarouselShelfRenderer
        val playlistsSection =
            findSectionByTitle("Playlists by $artistName")?.musicCarouselShelfRenderer
        val featuredPlaylistsSection = findSectionByTitle("Featured on")?.musicCarouselShelfRenderer
        val relatedArtistsSection =
            findSectionByTitle("Fans might also like")?.musicCarouselShelfRenderer

        Innertube.ArtistPage(
            name = artistName,
            description = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.description
                ?.text,
            thumbnail = (response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.foregroundThumbnail
                ?: response
                    .header
                    ?.musicImmersiveHeaderRenderer
                    ?.thumbnail)
                ?.musicThumbnailRenderer
                ?.thumbnail
                ?.thumbnails
                ?.getOrNull(0),
            shuffleEndpoint = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.playButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.watchEndpoint,
            radioEndpoint = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.startRadioButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.watchEndpoint,
            songs = songsSection
                ?.contents
                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull(Innertube.SongItem::from),
            songsEndpoint = songsSection
                ?.bottomEndpoint
                ?.browseEndpoint,
            albums = albumsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.AlbumItem::from),
            albumsEndpoint = albumsSection
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint,
            singles = singlesSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.AlbumItem::from),
            singlesEndpoint = singlesSection
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint,
            playlists = playlistsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.PlaylistItem::from),
            featuredPlaylists = featuredPlaylistsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.PlaylistItem::from),
            relatedArtists = relatedArtistsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.ArtistItem::from)
        )
    }