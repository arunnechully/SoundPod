package com.github.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ContinuationResponse(
    val continuationContents: ContinuationContents? = null,
    val onResponseReceivedActions: List<OnResponseReceivedAction>? = null,
) {
    @Serializable
    data class ContinuationContents(
        val musicShelfContinuation: MusicShelfRenderer? = null,
        val musicPlaylistShelfContinuation: MusicPlaylistShelfRenderer? = null,
        val sectionListContinuation: SectionListRenderer? = null,
        val playlistPanelContinuation: NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer? = null,
    )

    @Serializable
    data class OnResponseReceivedAction(
        val appendContinuationItemsAction: AppendContinuationItemsAction? = null,
        val reloadContinuationItemsCommand: ReloadContinuationItemsCommand? = null,
    ) {
        @Serializable
        data class AppendContinuationItemsAction(
            val continuationItems: List<ContinuationItem>? = null,
        )

        @Serializable
        data class ReloadContinuationItemsCommand(
            val continuationItems: List<ContinuationItem>? = null,
        )
    }

    @Serializable
    data class ContinuationItem(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val continuationItemRenderer: ContinuationItemRenderer? = null,
    )
}
