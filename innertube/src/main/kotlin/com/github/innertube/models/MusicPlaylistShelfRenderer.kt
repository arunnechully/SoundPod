package com.github.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicPlaylistShelfRenderer(
    val contents: List<Content>?,
    val continuations: List<Continuation>? = null,
    val playlistId: String? = null,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null
    )
}