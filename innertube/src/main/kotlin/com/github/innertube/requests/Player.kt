package com.github.innertube.requests

import com.github.innertube.Innertube
import com.github.innertube.models.PlayerResponse
import com.github.innertube.models.YouTubeClient
import com.github.innertube.models.bodies.PlayerBody
import com.github.innertube.models.bodies.ServiceIntegrityDimensions
import com.github.innertube.utils.runCatchingNonCancellable
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody

data class PlayerResult(
    val response: PlayerResponse,
    val userAgent: String
)

suspend fun Innertube.player(videoId: String): Result<PlayerResult>? = runCatchingNonCancellable {
    waitForSession(5000)
    
    // Try DYNAMIC client first if we have extracted context
    if (context != null) {
        val dynamicResponse = tryPlayer(videoId, YouTubeClient.DYNAMIC, useCookies = true)
        if (dynamicResponse?.playabilityStatus?.status == "OK") {
            println("Innertube: Successfully used DYNAMIC client for $videoId")
            return@runCatchingNonCancellable PlayerResult(
                response = dynamicResponse.applyDecipher(decipher, signatureDecipher),
                userAgent = YouTubeClient.DYNAMIC.userAgent
            )
        } else {
            println("Innertube: DYNAMIC client failed for $videoId: ${dynamicResponse?.playabilityStatus?.status}")
        }
    }

    val vrResponse = tryPlayer(videoId, YouTubeClient.ANDROID_VR, useCookies = false)
    if (vrResponse?.playabilityStatus?.status == "OK") {
        println("Innertube: Successfully used ANDROID_VR client for $videoId")
        return@runCatchingNonCancellable PlayerResult(
            response = vrResponse.applyDecipher(decipher, signatureDecipher),
            userAgent = YouTubeClient.ANDROID_VR.userAgent
        )
    }
    val tvResponse = tryPlayer(videoId, YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER, useCookies = false)
    if (tvResponse?.playabilityStatus?.status == "OK") {
        println("Innertube: Successfully used TVHTML5 client for $videoId")
        return@runCatchingNonCancellable PlayerResult(
            response = tvResponse.applyDecipher(decipher, signatureDecipher),
            userAgent = YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER.userAgent
        )
    }

    throw Exception("All Innertube player clients failed for $videoId")
}

private suspend fun Innertube.tryPlayer(
    videoId: String, 
    clientType: YouTubeClient, 
    useCookies: Boolean,
    extraHeaders: Map<String, String> = emptyMap(),
    includeThirdParty: Boolean = false,
    host: String = "www.youtube.com"
): PlayerResponse? = runCatching {
    client.post("https://$host/youtubei/v1/player") {
        header("User-Agent", clientType.userAgent)
        attributes.put(Innertube.Attributes.UseCookies, useCookies)
        extraHeaders.forEach { (key, value) -> header(key, value) }
        
        setBody(
            PlayerBody(
                context = clientType.toContext(visitorData = visitorData, includeThirdParty = includeThirdParty),
                videoId = videoId,
                serviceIntegrityDimensions = poToken?.let { ServiceIntegrityDimensions(poToken = it) }
            )
        )
        mask("playabilityStatus(status,reason,messages),playerConfig.audioConfig,streamingData.adaptiveFormats,streamingData.formats,videoDetails.videoId")
    }.body<PlayerResponse>()
}.getOrNull()

private suspend fun PlayerResponse.applyDecipher(
    decipherN: (suspend (String) -> String)?,
    decipherSig: (suspend (String) -> String)?
): PlayerResponse {
    if (streamingData == null) return this
    
    return copy(
        streamingData = streamingData.copy(
            adaptiveFormats = streamingData.adaptiveFormats?.map { format ->
                val url = format.url ?: format.signatureCipher?.let { parseSignatureCipher(it, decipherSig) }
                format.copy(url = url?.let { decipherUrl(it, decipherN) })
            },
            formats = streamingData.formats?.map { format ->
                val url = format.url ?: format.signatureCipher?.let { parseSignatureCipher(it, decipherSig) }
                format.copy(url = url?.let { decipherUrl(it, decipherN) })
            }
        )
    )
}

private suspend fun parseSignatureCipher(cipher: String, decipher: (suspend (String) -> String)?): String? {
    val params = cipher.split("&").associate { 
        val parts = it.split("=")
        parts[0] to java.net.URLDecoder.decode(parts[1], "UTF-8")
    }
    
    val baseUrl = params["url"] ?: return null
    val signature = params["s"] ?: return baseUrl
    val sp = params["sp"] ?: "sig"
    
    val decipheredSig = if (decipher != null) decipher(signature) else signature
    
    return if (baseUrl.contains("?")) {
        "$baseUrl&$sp=$decipheredSig"
    } else {
        "$baseUrl?$sp=$decipheredSig"
    }
}

private suspend fun decipherUrl(url: String, decipher: (suspend (String) -> String)?): String {
    if (decipher == null) return url
    val nParam = url.substringAfter("&n=", "").substringBefore("&")
    if (nParam.isEmpty()) return url
    
    val decipheredN = decipher(nParam)
    return url.replace("&n=$nParam", "&n=$decipheredN")
}
