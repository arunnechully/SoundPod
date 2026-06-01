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
        val poToken = Innertube.poToken?.takeIf { it.isNotEmpty() }?.let { ServiceIntegrityDimensions(poToken = it) }

        // Prioritize clients that often bypass throttling or are very fast
        val clients = listOf(
            YouTubeClient.ANDROID_VR, // Often has unthrottled streams
            YouTubeClient.ANDROID_MUSIC,
            YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
            YouTubeClient.WEB_REMIX
        )

        val deferredResponses = clients.map { ytClient ->
            async {
                runCatching {
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
            }
        }

        // Wait for all but we could potentially optimize this to return early if a 'perfect' one is found
        val responses = deferredResponses.awaitAll().filterNotNull()
        
        // Find the best response: 
        // 1. Must be OK status
        // 2. Prefer one with highest quality streams already present
        val bestResponse = responses.find { 
            it.playabilityStatus?.status == "OK" && it.streamingData?.highestQualityFormat != null 
        } ?: responses.find { 
            it.playabilityStatus?.status == "OK" 
        } ?: responses.firstOrNull() ?: throw Exception("Failed to get player response from all clients")

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
