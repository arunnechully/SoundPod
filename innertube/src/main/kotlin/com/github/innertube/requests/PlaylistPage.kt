package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.BrowseResponse
import com.github.innertube.models.ContinuationResponse
import com.github.innertube.models.MusicCarouselShelfRenderer
import com.github.innertube.models.MusicShelfRenderer
import com.github.innertube.models.bodies.BrowseBody
import com.github.innertube.models.bodies.ContinuationBody
import com.github.innertube.utils.from
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.playlistPage(
    browseId: String,
    params: String? = null
) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = browseId,
                params = params
            )
        )
    }.body<BrowseResponse>()

    val header = response
        .contents
        ?.twoColumnBrowseResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.firstOrNull()
        ?.musicResponsiveHeaderRenderer

    val contents = response
        .contents
        ?.twoColumnBrowseResultsRenderer
        ?.secondaryContents
        ?.sectionListRenderer
        ?.contents

    val musicShelfRenderer = contents
        ?.firstOrNull()
        ?.musicShelfRenderer

    val otherVersionsSection = if (contents?.size == 3) contents.getOrNull(1)
    else {
        val section = contents?.getOrNull(1)
        if (section?.musicCarouselShelfRenderer?.contents?.size == 10) null
        else section
    }

    val relatedAlbumsSection = if (contents?.size == 3) contents.getOrNull(2)
    else {
        val section = contents?.getOrNull(1)
        if (section?.musicCarouselShelfRenderer?.contents?.size == 10) section
        else null
    }

    Innertube.PlaylistOrAlbumPage(
        title = header
            ?.title
            ?.text,
        thumbnail = header
            ?.thumbnail
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.firstOrNull(),
        authors = header
            ?.straplineTextOne
            ?.splitBySeparator()
            ?.getOrNull(0)
            ?.map(Innertube::Info),
        year = header
            ?.subtitle
            ?.splitBySeparator()
            ?.getOrNull(1)
            ?.firstOrNull()
            ?.text,
        url = response
            .microformat
            ?.microformatDataRenderer
            ?.urlCanonical,
        songsPage = musicShelfRenderer
            ?.toSongsPage(),
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
        mask("continuationContents.musicPlaylistShelfContinuation(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)")
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toSongsPage()
}

private fun MusicShelfRenderer?.toSongsPage() =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )