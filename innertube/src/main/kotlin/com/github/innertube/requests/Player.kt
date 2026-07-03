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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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

suspend fun Innertube.player(videoId: String) = runCatchingNonCancellable {
    val response = client.post(PLAYER) {
        setBody(
            PlayerBody(
                context = YouTubeClient.ANDROID_VR.toContext(visitorData = visitorData),
                videoId = videoId,
                serviceIntegrityDimensions = poToken?.let { ServiceIntegrityDimensions(poToken = it) }
            )
        )
        mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,streamingData.formats,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (response.playabilityStatus?.status == "OK") {
        return@runCatchingNonCancellable response.applyDecipher(decipher)
    }
    else {
        val safePlayerResponse = client.post(PLAYER) {
            setBody(
                PlayerBody(
                    context = YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER.toContext(visitorData = visitorData).copy(
                        thirdParty = Context.ThirdParty(
                            embedUrl = "https://www.youtube.com/watch?v=$videoId"
                        )
                    ),
                    videoId = videoId
                )
            )
            mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,streamingData.formats,videoDetails.videoId")
        }.body<PlayerResponse>()

        if (safePlayerResponse.playabilityStatus?.status != "OK") {
            return@runCatchingNonCancellable response.applyDecipher(decipher)
        }

        val audioStreams = runCatching {
            client.get("https://pipedapi.adminforge.de/streams/$videoId") {
                contentType(ContentType.Application.Json)
            }.body<PipedResponse>().audioStreams
        }.getOrNull() ?: emptyList()

        if (audioStreams.isEmpty()) {
            return@runCatchingNonCancellable safePlayerResponse.applyDecipher(decipher)
        }

        safePlayerResponse.copy(
            streamingData = safePlayerResponse.streamingData?.copy(
                adaptiveFormats = safePlayerResponse.streamingData.adaptiveFormats?.map { adaptiveFormat ->
                    adaptiveFormat.copy(
                        url = audioStreams.minByOrNull {
                            val bitrate = adaptiveFormat.bitrate ?: 0L
                            if (bitrate == 0L) Long.MAX_VALUE
                            else kotlin.math.abs(it.bitrate - bitrate)
                        }?.url
                    )
                },
                formats = safePlayerResponse.streamingData.formats?.map { format ->
                    format.copy(
                        url = audioStreams.minByOrNull {
                            val bitrate = format.bitrate ?: 0L
                            if (bitrate == 0L) Long.MAX_VALUE
                            else kotlin.math.abs(it.bitrate - bitrate)
                        }?.url
                    )
                }
            )
        ).applyDecipher(decipher)
    }
}

private suspend fun PlayerResponse.applyDecipher(decipher: (suspend (String) -> String)?): PlayerResponse {
    if (decipher == null || streamingData == null) return this
    
    return copy(
        streamingData = streamingData.copy(
            adaptiveFormats = streamingData.adaptiveFormats?.map { format ->
                format.copy(url = format.url?.let { decipherUrl(it, decipher) })
            },
            formats = streamingData.formats?.map { format ->
                format.copy(url = format.url?.let { decipherUrl(it, decipher) })
            }
        )
    )
}

private suspend fun decipherUrl(url: String, decipher: suspend (String) -> String): String {
    val nParam = url.substringAfter("&n=", "").substringBefore("&")
    if (nParam.isEmpty()) return url
    
    val decipheredN = decipher(nParam)
    return url.replace("&n=$nParam", "&n=$decipheredN")
}
