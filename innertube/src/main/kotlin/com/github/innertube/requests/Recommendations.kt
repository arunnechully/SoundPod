package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.BrowseResponse
import com.github.innertube.models.MusicCarouselShelfRenderer
import com.github.innertube.models.bodies.BrowseBody
import com.github.innertube.utils.findSectionByTitle
import com.github.innertube.utils.from
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.recommendations(): Result<List<Innertube.SongItem>?>? = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                localized = false,
                browseId = "FEmusic_home"
            )
        )
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    (sectionListRenderer?.findSectionByTitle("Quick picks")
        ?: sectionListRenderer?.findSectionByTitle("Recommended")
        ?: sectionListRenderer?.findSectionByTitle("Listen again")
        ?: sectionListRenderer?.contents?.firstOrNull { it.musicCarouselShelfRenderer != null })
        ?.musicCarouselShelfRenderer
        ?.contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
        ?.mapNotNull { Innertube.SongItem.from(it) }
        ?.takeIf { it.isNotEmpty() }
}
