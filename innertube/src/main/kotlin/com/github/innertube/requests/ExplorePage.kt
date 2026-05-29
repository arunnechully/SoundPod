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

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = "FEcharts",
                context = YouTubeClient.WEB_REMIX.toContext(
                    hl = "en",
                    gl = Locale.getDefault().country.ifBlank { "US" },
                )
            )
        )
        mask("contents.sectionListRenderer.contents.musicCarouselShelfRenderer(header.musicCarouselShelfBasicHeaderRenderer(title),contents($MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK))")
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    sectionListRenderer
        ?.findSectionByTitle("Top songs")
        ?.musicCarouselShelfRenderer
        ?.contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
        ?.mapNotNull(Innertube.SongItem::from)
}
