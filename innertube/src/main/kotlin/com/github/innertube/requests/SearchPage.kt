package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.ContinuationResponse
import com.github.innertube.models.MusicShelfRenderer
import com.github.innertube.models.SearchResponse
import com.github.innertube.models.bodies.ContinuationBody
import com.github.innertube.models.bodies.SearchBody
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun <T : Innertube.Item> Innertube.searchPage(
    query: String,
    params: String,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingNonCancellable {
    val response = client.post(SEARCH) {
        setBody(
            SearchBody(
                query = query,
                params = params
            )
        )
        mask("contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)")
    }.body<SearchResponse>()

    response
        .contents
        ?.tabbedSearchResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.lastOrNull()
        ?.musicShelfRenderer
        ?.toItemsPage(fromMusicShelfRendererContent)
}

suspend fun <T : Innertube.Item> Innertube.searchPage(
    continuation: String,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingNonCancellable {
    val response = client.post(SEARCH) {
        setBody(ContinuationBody(continuation = continuation))
        mask("continuationContents.musicShelfContinuation(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)")
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toItemsPage(fromMusicShelfRendererContent)
}

private fun <T : Innertube.Item> MusicShelfRenderer?.toItemsPage(mapper: (MusicShelfRenderer.Content) -> T?) =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull(mapper),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )