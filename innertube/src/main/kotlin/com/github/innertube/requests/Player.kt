package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.github.innertube.Innertube
import com.github.innertube.models.Context
import com.github.innertube.models.PlayerResponse
import com.github.innertube.models.YouTubeClient
import com.github.innertube.models.bodies.PlayerBody
import com.github.innertube.utils.runCatchingNonCancellable
import kotlinx.serialization.Serializable

suspend fun Innertube.player(videoId: String) = runCatchingNonCancellable {
    val response = client.post(PLAYER) {
        setBody(
            PlayerBody(
                context = YouTubeClient.IOS.toContext(visitorData = visitorData),
                videoId = videoId
            )
        )
        mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (response.playabilityStatus?.status == "OK") response
    else {
        @Serializable
        data class AudioStream(
            val url: String,
            val bitrate: Long
        )

        @Serializable
        data class PipedResponse(
            val audioStreams: List<AudioStream>
        )

        val safePlayerResponse = client.post(PLAYER) {
            setBody(
                PlayerBody(
                    context = YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER.toContext().copy(
                        thirdParty = Context.ThirdParty(
                            embedUrl = "https://www.youtube.com/watch?v=$videoId"
                        )
                    ),
                    videoId = videoId
                )
            )
            mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,videoDetails.videoId")
        }.body<PlayerResponse>()

        if (safePlayerResponse.playabilityStatus?.status != "OK") {
            return@runCatchingNonCancellable response
        }

        val audioStreams = client.get("https://pipedapi.adminforge.de/streams/$videoId") {
            contentType(ContentType.Application.Json)
        }.body<PipedResponse>().audioStreams

        safePlayerResponse.copy(
            streamingData = safePlayerResponse.streamingData?.copy(
                adaptiveFormats = safePlayerResponse.streamingData.adaptiveFormats?.map { adaptiveFormat ->
                    adaptiveFormat.copy(
                        url = audioStreams.find { it.bitrate == adaptiveFormat.bitrate }?.url
                    )
                }
            )
        )
    }
}