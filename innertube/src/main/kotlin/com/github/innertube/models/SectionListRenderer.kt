package com.github.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SectionListRenderer(
    val contents: List<Content>?,
    val continuations: List<Continuation>?
) {
    @Serializable
    data class Content(
        @JsonNames("musicImmersiveCarouselShelfRenderer")
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer?,
        @JsonNames("musicPlaylistShelfRenderer")
        val musicShelfRenderer: MusicShelfRenderer?,
        val gridRenderer: GridRenderer?,
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer?
    ) {
        @Serializable
        data class MusicDescriptionShelfRenderer(
            val description: Runs?
        )

        @Serializable
        data class MusicResponsiveHeaderRenderer(
            val title: Runs?,
            val subtitle: Runs?,
            val secondSubtitle: Runs?,
            val thumbnail: ThumbnailRenderer?,
            val straplineTextOne: Runs?
        )
    }
}