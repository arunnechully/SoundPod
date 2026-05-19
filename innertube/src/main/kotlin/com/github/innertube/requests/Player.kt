package com.github.innertube.requests

import com.github.innertube.Innertube
import com.github.innertube.models.Context
import com.github.innertube.models.PlayerResponse
import com.github.innertube.models.YouTubeClient
import com.github.innertube.models.bodies.PlayerBody
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
    val token = visitorData().getOrNull()

    val vrResponse = client.post(PLAYER) {
        header("User-Agent", YouTubeClient.ANDROID_VR.userAgent)
        setBody(
            PlayerBody(
                context = YouTubeClient.ANDROID_VR.toContext(visitorData = token),
                videoId = videoId
            )
        )
        mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (vrResponse.playabilityStatus?.status == "OK" && vrResponse.streamingData?.adaptiveFormats?.isNotEmpty() == true) {
        return@runCatchingNonCancellable vrResponse
    }
    val safePlayerResponse = client.post(PLAYER) {
        header("User-Agent", YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER.userAgent)
        setBody(
            PlayerBody(
                context = YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER.toContext().copy(
                    thirdParty = Context.ThirdParty(embedUrl = "https://www.youtube.com/watch?v=$videoId")
                ),
                videoId = videoId
            )
        )
        mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,videoDetails.videoId")
    }.body<PlayerResponse>()


    val pipedInstances = listOf(
        "https://piped.private.coffee",
        "https://pipedapi.orangenet.cc",
        "https://piped-api.garudalinux.org",
        "https://piped-api.lunar.icu",
        "https://pipedapi.tokhmi.xyz",
        "https://pipedapi.smnz.de",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.kavin.rocks"
    ).shuffled()

    @Serializable data class AudioStream(val url: String, val bitrate: Long)
    @Serializable data class PipedResponse(val audioStreams: List<AudioStream>)

    var extractedStreams: List<AudioStream>? = null

    // Loop through the servers sequentially until one responds with audio
    for (baseUrl in pipedInstances) {
        try {
            val pipedResponse = client.get("$baseUrl/streams/$videoId") {
                contentType(ContentType.Application.Json)
            }.body<PipedResponse>()

            if (pipedResponse.audioStreams.isNotEmpty()) {
                extractedStreams = pipedResponse.audioStreams
                println("SoundPod: Audio extracted via -> $baseUrl")
                break
            }
        } catch (_: Exception) {
            continue
        }
    }

    if (extractedStreams == null) {
        println("SoundPod: CRITICAL - All Piped fallbacks exhausted.")
        return@runCatchingNonCancellable safePlayerResponse
    }

    safePlayerResponse.copy(
        streamingData = safePlayerResponse.streamingData?.copy(
            adaptiveFormats = safePlayerResponse.streamingData.adaptiveFormats?.map { adaptiveFormat ->
                adaptiveFormat.copy(
                    url = extractedStreams.find { it.bitrate == adaptiveFormat.bitrate }?.url ?: adaptiveFormat.url
                )
            }
        ),
        playabilityStatus = safePlayerResponse.playabilityStatus?.copy(status = "OK")
    )
}