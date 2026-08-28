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
    val clients = listOf(
        YouTubeClient.ANDROID_VR,
        YouTubeClient.VISION_OS,
        YouTubeClient.ANDROID_EMBEDDED_PLAYER,
        YouTubeClient.ANDROID_MUSIC
    )

    for (clientType in clients) {
        val response = client.post("https://www.youtube.com$PLAYER") {
            header("User-Agent", clientType.userAgent)
            header("X-YouTube-Client-Name", clientType.clientId ?: clientType.clientName)
            header("X-YouTube-Client-Version", clientType.clientVersion)
            setBody(
                PlayerBody(
                    context = clientType.toContext(visitorData = visitorData),
                    videoId = videoId,
                    serviceIntegrityDimensions = poToken?.let { ServiceIntegrityDimensions(poToken = it) }
                )
            )
            mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,streamingData.formats,videoDetails.videoId")
        }.body<PlayerResponse>()

        if (response.playabilityStatus?.status == "OK" && response.streamingData?.highestQualityFormat != null) {
            return@runCatchingNonCancellable response.applyDecipher(decipher)
        }
    }

    val finalFallbackClient = YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER
    val finalFallbackResponse = client.post("https://www.youtube.com$PLAYER") {
        header("User-Agent", finalFallbackClient.userAgent)
        setBody(
            PlayerBody(
                context = finalFallbackClient.toContext(visitorData = visitorData).copy(
                    thirdParty = Context.ThirdParty(
                        embedUrl = "https://www.youtube.com/watch?v=$videoId"
                    )
                ),
                videoId = videoId
            )
        )
        mask("playabilityStatus.status,playerConfig.audioConfig,streamingData.adaptiveFormats,streamingData.formats,videoDetails.videoId")
    }.body<PlayerResponse>()

    return@runCatchingNonCancellable finalFallbackResponse.applyDecipher(decipher)
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
