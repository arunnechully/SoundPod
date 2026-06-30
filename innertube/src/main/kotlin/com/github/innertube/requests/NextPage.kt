package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.ContinuationResponse
import com.github.innertube.models.NextResponse
import com.github.innertube.models.bodies.ContinuationBody
import com.github.innertube.models.bodies.NextBody
import com.github.innertube.utils.from
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.nextPage(
    videoId: String? = null,
    playlistId: String? = null,
    params: String? = null,
    playlistSetVideoId: String? = null
): Result<Innertube.NextPage>? =
    runCatchingNonCancellable {
        val response = client.post(NEXT) {
            setBody(
                NextBody(
                    videoId = videoId,
                    playlistId = playlistId,
                    params = params,
                    playlistSetVideoId = playlistSetVideoId
                )
            )
            mask("contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer.content.musicQueueRenderer.content.playlistPanelRenderer(continuations,contents(automixPreviewVideoRenderer,$PLAYLIST_PANEL_VIDEO_RENDERER_MASK))")
        }.body<NextResponse>()

        val tabs = response
            .contents
            ?.singleColumnMusicWatchNextResultsRenderer
            ?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer
            ?.tabs

        val playlistPanelRenderer = tabs
            ?.getOrNull(0)
            ?.tabRenderer
            ?.content
            ?.musicQueueRenderer
            ?.content
            ?.playlistPanelRenderer

        if (playlistId == null) {
            val endpoint = playlistPanelRenderer
                ?.contents
                ?.lastOrNull()
                ?.automixPreviewVideoRenderer
                ?.content
                ?.automixPlaylistVideoRenderer
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint

            if (endpoint != null) {
                return nextPage(
                    videoId = videoId,
                    playlistId = endpoint.playlistId,
                    params = endpoint.params,
                    playlistSetVideoId = playlistSetVideoId
                )
            }
        }

        Innertube.NextPage(
            playlistId = playlistId,
            playlistSetVideoId = playlistSetVideoId,
            params = params,
            itemsPage = playlistPanelRenderer
                ?.toSongsPage()
        )
    }

suspend fun Innertube.nextPage(continuation: String) = runCatchingNonCancellable {
    val response = client.post(NEXT) {
        setBody(ContinuationBody(continuation = continuation))
        mask("continuationContents.playlistPanelContinuation(continuations,contents.$PLAYLIST_PANEL_VIDEO_RENDERER_MASK)")
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.playlistPanelContinuation
        ?.toSongsPage()
}

private fun NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer?.toSongsPage() =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull(NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer.Content::playlistPanelVideoRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )