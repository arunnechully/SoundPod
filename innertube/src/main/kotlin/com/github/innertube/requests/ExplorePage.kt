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
import kotlinx.coroutines.CancellationException
import java.util.Locale

suspend fun Innertube.charts(): Result<List<Innertube.SongItem>?>? = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        waitForSession(timeoutMs = 10000)
    }

    suspend fun fetchCharts(browseId: String): List<Innertube.SongItem>? {
        try {
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

            val sectionListRenderer = response.contents?.sectionListRenderer
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs
                    ?.firstNotNullOfOrNull { it.tabRenderer?.content?.sectionListRenderer }

            val preferredSections = listOfNotNull(
                sectionListRenderer?.findSectionByTitle("Top songs"),
                sectionListRenderer?.findSectionByTitle("Top music videos"),
                sectionListRenderer?.findSectionByTitle("Trending")
            )

            return (preferredSections + sectionListRenderer?.contents.orEmpty()).firstNotNullOfOrNull { section ->
                section.musicShelfRenderer?.contents
                    ?.mapNotNull(Innertube.SongItem::from)
                    ?.takeIf { it.isNotEmpty() }
                    ?: section.musicCarouselShelfRenderer?.contents
                        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
                        ?.mapNotNull(Innertube.SongItem::from)
                        ?.takeIf { it.isNotEmpty() }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return null
        }
    }

    fetchCharts("FEmusic_charts") ?: fetchCharts("FEmusic_home") ?: fetchCharts("FEmusic_explore") ?: fetchCharts("FEcharts")
}

suspend fun Innertube.newReleases(): Result<List<Innertube.AlbumItem>?>? = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        waitForSession(timeoutMs = 10000)
    }

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = "FEmusic_new_releases",
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

    (sectionListRenderer?.findSectionByTitle("New albums & singles")
        ?: sectionListRenderer?.contents?.firstOrNull { it.musicCarouselShelfRenderer != null })
        ?.musicCarouselShelfRenderer
        ?.contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
        ?.mapNotNull(Innertube.AlbumItem::from)
        ?.takeIf { it.isNotEmpty() }
}
