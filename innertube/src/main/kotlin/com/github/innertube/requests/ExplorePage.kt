package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.BrowseResponse
import com.github.innertube.models.MusicCarouselShelfRenderer
import com.github.innertube.models.YouTubeClient
import com.github.innertube.models.bodies.BrowseBody
import com.github.innertube.utils.findSectionByTitle
import com.github.innertube.utils.from
import com.github.innertube.utils.runCatchingNonCancellable
import java.util.Locale

suspend fun Innertube.charts(): Result<List<Innertube.SongItem>?>? = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        waitForSession(timeoutMs = 10000)
    }

    suspend fun fetchCharts(browseId: String): List<Innertube.SongItem>? {
        val response = client.post(BROWSE) {
            setBody(
                BrowseBody(
                    browseId = browseId,
                    context = YouTubeClient.WEB_REMIX.toContext(
                        hl = "en",
                        gl = Locale.getDefault().country.ifBlank { "US" },
                    )
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
            ?.mapNotNull(Innertube.SongItem::from)
            ?.takeIf { it.isNotEmpty() }
    }

    fetchCharts("FEcharts") ?: fetchCharts("FEmusic_charts") ?: fetchCharts("FEmusic_home") ?: fetchCharts("FEmusic_explore")
}
