package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.BrowseResponse
import com.github.innertube.models.ContinuationResponse
import com.github.innertube.models.MusicCarouselShelfRenderer
import com.github.innertube.models.MusicShelfRenderer
import com.github.innertube.models.MusicPlaylistShelfRenderer
import com.github.innertube.models.NavigationEndpoint
import com.github.innertube.models.Runs
import com.github.innertube.models.bodies.BrowseBody
import com.github.innertube.models.bodies.ContinuationBody
import com.github.innertube.utils.from
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.playlistPage(
    browseId: String,
    params: String? = null
) = runCatchingNonCancellable {
    val cleanId = if (browseId.contains("list=")) {
        browseId.substringAfter("list=").substringBefore("&")
    } else {
        browseId
    }.removePrefix("VL")

    val actualBrowseId = if (cleanId.startsWith("VL")) {
        cleanId
    } else if (cleanId.startsWith("MPRE") || cleanId.startsWith("UC") || cleanId.startsWith("FE")) {
        cleanId
    } else if (browseId.contains("list=") || cleanId.startsWith("PL") || cleanId.startsWith("RD") || cleanId.startsWith("OLAK") || cleanId.startsWith("LM")) {
        "VL$cleanId"
    } else {
        cleanId
    }

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = actualBrowseId,
                params = params
            )
        )
    }.body<BrowseResponse>()

    val tabSectionContents = response
        .contents
        ?.twoColumnBrowseResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?: response
            .contents
            ?.singleColumnBrowseResultsRenderer
            ?.tabs
            ?.firstOrNull()
            ?.tabRenderer
            ?.content
            ?.sectionListRenderer
            ?.contents

    val secondarySectionContents = response
        .contents
        ?.twoColumnBrowseResultsRenderer
        ?.secondaryContents
        ?.sectionListRenderer
        ?.contents

    val rootSectionContents = response
        .contents
        ?.sectionListRenderer
        ?.contents

    val allSectionContents = listOfNotNull(
        secondarySectionContents,
        tabSectionContents,
        rootSectionContents
    ).flatten()

    // Standard playlists (PL/VL)
    val musicShelfRenderer = allSectionContents
        .firstNotNullOfOrNull { it.musicShelfRenderer }

    // Mixes and Charts (RDCLAK/Mixes)
    val musicPlaylistShelfRenderer = allSectionContents
        .firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }

    val otherVersionsSection = secondarySectionContents?.let { contents ->
        if (contents.size == 3) contents.getOrNull(1)
        else {
            val section = contents.getOrNull(1)
            if (section?.musicCarouselShelfRenderer?.contents?.size == 10) null
            else section
        }
    }

    val relatedAlbumsSection = secondarySectionContents?.let { contents ->
        if (contents.size == 3) contents.getOrNull(2)
        else {
            val section = contents.getOrNull(1)
            if (section?.musicCarouselShelfRenderer?.contents?.size == 10) section
            else null
        }
    }

    val responsiveHeader = allSectionContents
        .firstNotNullOfOrNull {
            it.musicResponsiveHeaderRenderer
                ?: it.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
        }
        ?: response.header?.musicResponsiveHeaderRenderer
        ?: response.header?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer

    val detailHeader = response.header?.musicDetailHeaderRenderer

    val immersiveHeader = response.header?.musicImmersiveHeaderRenderer

    val title = Innertube.Info.cleanName(
        responsiveHeader?.title?.text
            ?: detailHeader?.title?.text
            ?: immersiveHeader?.title?.text
            ?: musicShelfRenderer?.title?.text
    )

    val thumbnail = responsiveHeader
        ?.thumbnail
        ?.musicThumbnailRenderer
        ?.thumbnail
        ?.thumbnails
        ?.firstOrNull()
        ?: detailHeader
            ?.thumbnail
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.firstOrNull()
        ?: immersiveHeader
            ?.thumbnail
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.firstOrNull()

    val authors = (responsiveHeader
        ?.straplineTextOne
        ?: detailHeader?.subtitle)
        ?.splitBySeparator()
        ?.getOrNull(0)
        ?.map<Runs.Run, Innertube.Info<NavigationEndpoint.Endpoint.Browse>>(Innertube::Info)

    val year = responsiveHeader
        ?.subtitle
        ?.splitBySeparator()
        ?.getOrNull(1)
        ?.firstOrNull()
        ?.text
        ?: detailHeader
            ?.subtitle
            ?.splitBySeparator()
            ?.getOrNull(1)
            ?.firstOrNull()
            ?.text

    Innertube.PlaylistOrAlbumPage(
        title = title,
        thumbnail = thumbnail,
        authors = authors,
        year = year,
        url = response
            .microformat
            ?.microformatDataRenderer
            ?.urlCanonical,
        songsPage = musicShelfRenderer?.toSongsPage()
            ?: musicPlaylistShelfRenderer?.toSongsPage()
            ?: Innertube.ItemsPage(items = emptyList(), continuation = null),
        otherVersions = otherVersionsSection
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        relatedAlbums = relatedAlbumsSection
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from)
    )
}

suspend fun Innertube.playlistPageContinuation(continuation: String) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(ContinuationBody(continuation = continuation))
    }.body<ContinuationResponse>()

    val continuationItems = response
        .onResponseReceivedActions
        ?.firstNotNullOfOrNull { action ->
            action.appendContinuationItemsAction?.continuationItems
                ?: action.reloadContinuationItemsCommand?.continuationItems
        }

    if (continuationItems != null) {
        val songs = continuationItems
            .mapNotNull { it.musicResponsiveListItemRenderer }
            .mapNotNull(Innertube.SongItem::from)

        val nextContinuation = continuationItems
            .firstNotNullOfOrNull { it.continuationItemRenderer?.continuation }

        return@runCatchingNonCancellable Innertube.ItemsPage(
            items = songs,
            continuation = nextContinuation
        )
    }

    val fallbackContinuation = response
        .continuationContents
        ?.sectionListContinuation
        ?.continuations
        ?.firstOrNull()
        ?.continuation

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toSongsPage(fallbackContinuation)
        ?: response
            .continuationContents
            ?.musicPlaylistShelfContinuation
            ?.toSongsPage(fallbackContinuation)
        ?: response
            .continuationContents
            ?.sectionListContinuation
            ?.contents
            ?.firstNotNullOfOrNull { content ->
                content.musicShelfRenderer?.toSongsPage(fallbackContinuation)
                    ?: content.musicPlaylistShelfRenderer?.toSongsPage(fallbackContinuation)
            }
}

/**
 * Standard Shelf Converter (PL/VL)
 */
private fun MusicShelfRenderer?.toSongsPage(fallbackContinuation: String? = null) =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.continuation
            ?: this
                ?.contents
                ?.firstNotNullOfOrNull { it.continuationItemRenderer?.continuation }
            ?: fallbackContinuation
    )

/**
 * Mix/Chart Shelf Converter (RDCLAK)
 */
private fun MusicPlaylistShelfRenderer?.toSongsPage(fallbackContinuation: String? = null) =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull { it.musicResponsiveListItemRenderer }
            ?.mapNotNull(Innertube.SongItem::from),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.continuation
            ?: this
                ?.contents
                ?.firstNotNullOfOrNull { it.continuationItemRenderer?.continuation }
            ?: fallbackContinuation
    )
