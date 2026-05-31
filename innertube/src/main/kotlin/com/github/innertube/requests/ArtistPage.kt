package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.BrowseResponse
import com.github.innertube.models.MusicCarouselShelfRenderer
import com.github.innertube.models.MusicPlaylistShelfRenderer
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
            val tabs = (response.contents?.singleColumnBrowseResultsRenderer?.tabs
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs)

            tabs?.forEach { tab ->
                tab.tabRenderer?.content?.sectionListRenderer?.findSectionByTitle(text)?.let { return it }
            }
            return null
        }

        val artistName = Innertube.Info.cleanName(response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.title
            ?.text)

        val songsSection = (findSectionByTitle("Top songs")
            ?: findSectionByTitle("Songs"))
        val songsShelf = songsSection?.musicShelfRenderer
        val songsPlaylistShelf = songsSection?.musicPlaylistShelfRenderer

        val albumsSection = (findSectionByTitle("Albums")
            ?: findSectionByTitle("Discography"))?.musicCarouselShelfRenderer
        val singlesSection = (findSectionByTitle("Singles & EPs")
            ?: findSectionByTitle("Singles")
            ?: findSectionByTitle("Singles & albums"))?.musicCarouselShelfRenderer
        val playlistsSection = (findSectionByTitle("Playlists by $artistName")
            ?: findSectionByTitle("Playlists"))?.musicCarouselShelfRenderer
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
            songs = songsShelf
                ?.contents
                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull { Innertube.SongItem.from(it) }
                ?: songsPlaylistShelf
                    ?.contents
                    ?.mapNotNull(MusicPlaylistShelfRenderer.Content::musicResponsiveListItemRenderer)
                    ?.mapNotNull { Innertube.SongItem.from(it) },
            songsEndpoint = songsShelf
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