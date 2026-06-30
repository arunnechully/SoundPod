package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import com.github.innertube.Innertube
import com.github.innertube.models.GetQueueResponse
import com.github.innertube.models.bodies.QueueBody
import com.github.innertube.utils.from
import com.github.innertube.utils.runCatchingNonCancellable

suspend fun Innertube.queue(videoIds: List<String>) = runCatchingNonCancellable {
    val response = client.post(QUEUE) {
        setBody(QueueBody(videoIds = videoIds))
        mask("queueDatas.content.$PLAYLIST_PANEL_VIDEO_RENDERER_MASK")
    }.body<GetQueueResponse>()

    response
        .queueDatas
        ?.mapNotNull { queueData ->
            queueData
                .content
                ?.playlistPanelVideoRenderer
                ?.let(Innertube.SongItem::from)
        }
}

suspend fun Innertube.song(videoId: String): Result<Innertube.SongItem?>? =
    queue(videoIds = listOf(videoId))?.map { it?.firstOrNull() }