package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.BrowseResponse
import com.github.innertube.models.ContinuationResponse
import com.github.innertube.models.GridRenderer
import com.github.innertube.models.MusicPlaylistShelfRenderer
import com.github.innertube.models.MusicResponsiveListItemRenderer
import com.github.innertube.models.MusicShelfRenderer
import com.github.innertube.models.MusicTwoRowItemRenderer
import com.github.innertube.models.bodies.BrowseBody
import com.github.innertube.models.bodies.ContinuationBody
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun <T : Innertube.Item> Innertube.itemsPage(
    browseId: String,
    params: String?,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null },
) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = browseId,
                params = params
            )
        )
    }.body<BrowseResponse>()

    val sectionListRenderer = (response.contents?.singleColumnBrowseResultsRenderer?.tabs
        ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs)
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer

    val shelves = sectionListRenderer?.contents
        ?.filter { it.musicShelfRenderer != null || it.musicPlaylistShelfRenderer != null || it.gridRenderer != null }
        ?: emptyList()

    if (shelves.isEmpty()) return@runCatchingNonCancellable null

    val items = mutableListOf<T>()
    var continuation: String? = sectionListRenderer?.continuations
        ?.firstOrNull()
        ?.nextContinuationData
        ?.continuation

    shelves.forEach { shelf ->
        val page = itemsPageFromMusicShelRendererOrGridRenderer(
            musicShelfRenderer = shelf.musicShelfRenderer,
            musicPlaylistShelfRenderer = shelf.musicPlaylistShelfRenderer,
            gridRenderer = shelf.gridRenderer,
            fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
            fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer,
        )
        page?.items?.let { items.addAll(it) }
        if (continuation == null) continuation = page?.continuation
    }

    Innertube.ItemsPage(
        items = items.ifEmpty { null },
        continuation = continuation
    )
}

suspend fun <T : Innertube.Item> Innertube.itemsPageContinuation(
    continuation: String,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null },
) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(ContinuationBody(continuation = continuation))
    }.body<ContinuationResponse>()

    val items = mutableListOf<T>()
    var nextContinuation: String? = null

    response.continuationContents?.let { contents ->
        contents.musicShelfContinuation?.let { shelf ->
            val page = itemsPageFromMusicShelRendererOrGridRenderer(
                musicShelfRenderer = shelf,
                musicPlaylistShelfRenderer = null,
                gridRenderer = null,
                fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
                fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer,
            )
            page?.items?.let { items.addAll(it) }
            nextContinuation = page?.continuation
        }

        contents.sectionListContinuation?.let { sectionList ->
            nextContinuation = sectionList.continuations
                ?.firstOrNull()
                ?.nextContinuationData
                ?.continuation

            sectionList.contents?.forEach { shelf ->
                val page = itemsPageFromMusicShelRendererOrGridRenderer(
                    musicShelfRenderer = shelf.musicShelfRenderer,
                    musicPlaylistShelfRenderer = shelf.musicPlaylistShelfRenderer,
                    gridRenderer = shelf.gridRenderer,
                    fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
                    fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer,
                )
                page?.items?.let { items.addAll(it) }
                if (nextContinuation == null) nextContinuation = page?.continuation
            }
        }
    }

    Innertube.ItemsPage(
        items = items.ifEmpty { null },
        continuation = nextContinuation
    )
}

private fun <T : Innertube.Item> itemsPageFromMusicShelRendererOrGridRenderer(
    musicShelfRenderer: MusicShelfRenderer?,
    musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer? = null,
    gridRenderer: GridRenderer?,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T?,
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T?,
): Innertube.ItemsPage<T>? {
    return if (musicShelfRenderer != null) {
        Innertube.ItemsPage(
            continuation = musicShelfRenderer
                .continuations
                ?.firstOrNull()
                ?.nextContinuationData
                ?.continuation,
            items = musicShelfRenderer
                .contents
                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull(fromMusicResponsiveListItemRenderer)
        )
    } else if (musicPlaylistShelfRenderer != null) {
        Innertube.ItemsPage(
            continuation = musicPlaylistShelfRenderer
                .continuations
                ?.firstOrNull()
                ?.nextContinuationData
                ?.continuation,
            items = musicPlaylistShelfRenderer
                .contents
                ?.mapNotNull(MusicPlaylistShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull(fromMusicResponsiveListItemRenderer)
        )
    } else if (gridRenderer != null) {
        Innertube.ItemsPage(
            continuation = null,
            items = gridRenderer
                .items
                ?.mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                ?.mapNotNull(fromMusicTwoRowItemRenderer)
        )
    } else null
}