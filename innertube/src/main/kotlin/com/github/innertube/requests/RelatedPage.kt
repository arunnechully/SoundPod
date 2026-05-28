package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.BrowseResponse
import com.github.innertube.models.MusicCarouselShelfRenderer
import com.github.innertube.models.NextResponse
import com.github.innertube.models.bodies.BrowseBody
import com.github.innertube.models.bodies.NextBody
import com.github.innertube.utils.from
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.relatedPage(videoId: String) = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        waitForSession(timeoutMs = 10000)
    }

    val nextResponse = client.post(NEXT) {
        setBody(NextBody(videoId = videoId))
        mask("contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer(endpoint,title)")
    }.body<NextResponse>()

    val browseId = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.getOrNull(2)
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId
        ?: return@runCatchingNonCancellable null

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                localized = true,
                browseId = browseId
            )
        )
        mask("contents.sectionListRenderer.contents.musicCarouselShelfRenderer(header.musicCarouselShelfBasicHeaderRenderer(title,strapline),contents($MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK,$MUSIC_TWO_ROW_ITEM_RENDERER_MASK))")
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    val carousels = sectionListRenderer
        ?.contents
        ?.mapNotNull { it.musicCarouselShelfRenderer }
        ?: emptyList()

    Innertube.RelatedPage(
        songs = carousels
            .firstOrNull { carousel ->
                carousel.contents?.any { it.musicResponsiveListItemRenderer != null } == true
            }
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        playlists = carousels
            .filter { carousel ->
                carousel.contents?.any { it.musicTwoRowItemRenderer != null } == true
            }
            .map { carousel ->
                carousel.contents?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                    ?.mapNotNull(Innertube.PlaylistItem::from)
            }
            .firstOrNull { !it.isNullOrEmpty() }
            ?.sortedByDescending { it.channel?.name == "YouTube Music" },
        albums = carousels
            .filter { carousel ->
                carousel.header?.musicCarouselShelfBasicHeaderRenderer?.strapline != null
            }
            .map { carousel ->
                carousel.contents?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                    ?.mapNotNull(Innertube.AlbumItem::from)
            }
            .firstOrNull { !it.isNullOrEmpty() },
        artists = carousels
            .filter { carousel ->
                carousel.contents?.any { it.musicTwoRowItemRenderer != null } == true
            }
            .map { carousel ->
                carousel.contents?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                    ?.mapNotNull(Innertube.ArtistItem::from)
            }
            .lastOrNull { !it.isNullOrEmpty() },
    )
}
