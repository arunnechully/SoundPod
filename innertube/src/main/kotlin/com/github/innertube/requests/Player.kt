package com.github.innertube.requests

import com.github.innertube.Innertube
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

@Serializable
private data class AudioStream(
    val url: String,
    val bitrate: Long
)

@Serializable
private data class PipedResponse(
    val audioStreams: List<AudioStream>
)

suspend fun Innertube.player(videoId: String) = coroutineScope {
    runCatchingNonCancellable {
        if (!hasRequiredTokens) {
            val ready = waitForSession(timeoutMs = 10000)
            if (!ready) {
                throw Exception("Tokens not ready: Ghost WebView failed to capture tokens in time.")
            }
        }

        val token = visitorData
        val poToken = (Innertube.poToken ?: Innertube.poTokenResolver?.getPoToken(videoId))
            ?.takeIf { it.isNotEmpty() }?.let { ServiceIntegrityDimensions(poToken = it) }

        // Prioritize clients that often bypass throttling or are very fast
        val clients = listOf(
            YouTubeClient.ANDROID_MUSIC,
            YouTubeClient.ANDROID_VR,
            YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
            YouTubeClient.WEB_REMIX
        )

        var bestResponse: PlayerResponse? = null
        
        for (ytClient in clients) {
            val response = runCatching {
                client.post(PLAYER) {
                    header("User-Agent", ytClient.userAgent)
                    setBody(
                        PlayerBody(
                            context = ytClient.toContext(visitorData = token).let { ctx ->
                                if (ytClient == YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER) {
                                    ctx.copy(
                                        thirdParty = com.github.innertube.models.Context.ThirdParty(
                                            embedUrl = "https://www.youtube.com/watch?v=$videoId"
                                        )
                                    )
                                } else ctx
                            },
                            videoId = videoId,
                            serviceIntegrityDimensions = poToken
                        )
                    )
                    mask("playabilityStatus,playerConfig.audioConfig,streamingData.adaptiveFormats,streamingData.formats,videoDetails")
                }.body<PlayerResponse>()
            }.getOrNull()

            if (response?.playabilityStatus?.status == "OK" && response.streamingData?.highestQualityFormat != null) {
                bestResponse = response
                break
            }
            
            if (response?.playabilityStatus?.status == "OK") {
                bestResponse = response
            }
        }

        if (bestResponse == null) {
            throw Exception("Failed to get player response from all clients")
        }

        // Piped fallback (only if YouTube failed completely)
        val audioStreams = if (bestResponse.streamingData?.highestQualityFormat == null) {
            runCatching {
                client.get("https://pipedapi.adminforge.de/streams/$videoId") {
                    contentType(ContentType.Application.Json)
                }.body<PipedResponse>().audioStreams
            }.getOrNull() ?: emptyList()
        } else emptyList()

        if (audioStreams.isNotEmpty()) {
            bestResponse.copy(
                streamingData = bestResponse.streamingData?.let { sd ->
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
        } else {
            bestResponse
        }
    }
}
