package com.github.innertube.requests

import com.github.innertube.Innertube
import com.github.innertube.models.Context
import com.github.innertube.models.PlayerResponse
import com.github.innertube.models.YouTubeClient
import com.github.innertube.models.bodies.PlayerBody
import com.github.innertube.models.bodies.ServiceIntegrityDimensions
import com.github.innertube.utils.runCatchingNonCancellable
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

suspend fun Innertube.player(videoId: String) = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        val ready = waitForSession(timeoutMs = 10000)
        if (!ready) {
            throw Exception("Tokens not ready: Ghost WebView failed to capture tokens in time.")
        }
    }

    val token = visitorData

    val clients = listOf(
        YouTubeClient.ANDROID_MUSIC,
        YouTubeClient.WEB_REMIX,
        YouTubeClient.ANDROID_VR,
        YouTubeClient.ANDROID_TESTSUITE,
        YouTubeClient.IOS,
        YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        YouTubeClient.ANDROID
    )

    var bestResponse: PlayerResponse? = null

    val poToken = Innertube.poToken?.takeIf { it.isNotEmpty() }?.let { ServiceIntegrityDimensions(poToken = it) }

    for (ytClient in clients) {
        val response = runCatching {
            client.post(PLAYER) {
                header("User-Agent", ytClient.userAgent)
                setBody(
                    PlayerBody(
                        context = ytClient.toContext(visitorData = token).let { ctx ->
                            if (ytClient == YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER) {
                                ctx.copy(
                                    thirdParty = Context.ThirdParty(
                                        embedUrl = "https://www.youtube.com/watch?v=$videoId"
                                    )
                                )
                            } else ctx
                        },
                        videoId = videoId,
                        serviceIntegrityDimensions = poToken
                    )
                )
                mask("playabilityStatus.status,playabilityStatus.reason,playerConfig.audioConfig,streamingData.adaptiveFormats,streamingData.formats,videoDetails.videoId,videoDetails.thumbnail")
            }.body<PlayerResponse>()
        }.getOrNull()

        if (response != null) {
            val status = response.playabilityStatus?.status
            val hasUrl = response.streamingData?.highestQualityFormat != null

            if (status == "OK" && hasUrl) {
                bestResponse = response
                break
            }
            if (status == "OK" || (bestResponse?.playabilityStatus?.status != "OK" && status != null)) {
                if (bestResponse == null || (status == "OK" && bestResponse.playabilityStatus?.status != "OK")) {
                    bestResponse = response
                }
            }
        }
    }

    val baseResponse = bestResponse ?: throw Exception("Failed to get player response from all clients")

    @Serializable
    data class AudioStream(
        val url: String,
        val bitrate: Long
    )

    @Serializable
    data class PipedResponse(
        val audioStreams: List<AudioStream>
    )

    // Piped URL extraction (only if YouTube failed to provide a playable format)
    val audioStreams = if (baseResponse.streamingData?.highestQualityFormat == null) {
        runCatching {
            client.get("https://pipedapi.adminforge.de/streams/$videoId") {
                contentType(ContentType.Application.Json)
            }.body<PipedResponse>().audioStreams
        }.getOrNull() ?: emptyList()
    } else emptyList()

    baseResponse.copy(
        streamingData = baseResponse.streamingData?.let { sd ->
            sd.copy(
                adaptiveFormats = sd.adaptiveFormats?.map { af ->
                    af.copy(
                        url = audioStreams.find { it.bitrate == af.bitrate }?.url ?: af.url
                    )
                },
                formats = sd.formats?.map { f ->
                    f.copy(
                        url = audioStreams.find { it.bitrate == f.bitrate }?.url ?: f.url
                    )
                }
            )
        }
    )
}