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

suspend fun Innertube.charts(): Result<List<Innertube.SongItem>?>? = runCatchingNonCancellable {
    suspend fun fetchCharts(browseId: String): List<Innertube.SongItem>? {
        val response = client.post(BROWSE) {
            setBody(
                BrowseBody(
                    localized = false,
                    browseId = browseId
                )
            )
        }.body<BrowseResponse>()

        val sectionListRenderer = response
            .contents
            ?.sectionListRenderer

        return (sectionListRenderer?.findSectionByTitle("Top songs")
            ?: sectionListRenderer?.findSectionByTitle("Top music videos")
            ?: sectionListRenderer?.findSectionByTitle("Trending")
            ?: sectionListRenderer?.contents?.firstOrNull { it.musicCarouselShelfRenderer != null })
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull { Innertube.SongItem.from(it) }
            ?.takeIf { it.isNotEmpty() }
    }

    fetchCharts("FEcharts") ?: fetchCharts("FEmusic_charts")
}
