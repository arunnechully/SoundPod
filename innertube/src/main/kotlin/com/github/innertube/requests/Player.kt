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

private const val TAG = "SoundPod-Player"

suspend fun Innertube.player(videoId: String) = runCatchingNonCancellable {
    println("$TAG: Starting player request for videoId: $videoId")
    
    // Ensure we have a session before proceeding
    waitForSession(5000)

    // TIER 1: WEB_REMIX (Exact match for WebView)
    val webRemixResponse = tryPlayer(videoId, YouTubeClient.WEB_REMIX, useCookies = true, extraHeaders = mapOf(
        "Sec-Fetch-Mode" to "cors",
        "Sec-Fetch-Site" to "same-origin",
        "X-Youtube-Client-Name" to "67",
        "X-Youtube-Client-Version" to (clientVersion ?: YouTubeClient.WEB_REMIX.clientVersion),
        "Origin" to "https://music.youtube.com",
        "Referer" to "https://music.youtube.com/"
    ))
    
    if (webRemixResponse?.playabilityStatus?.status == "OK") {
        println("$TAG: Successfully resolved player via WEB_REMIX")
        return@runCatchingNonCancellable webRemixResponse.applyDecipher(decipher)
    }
    logFailure("WEB_REMIX", webRemixResponse)

    // TIER 2: ANDROID_VR (The most reliable workaround)
    val vrResponse = tryPlayer(videoId, YouTubeClient.ANDROID_VR, useCookies = false)
    if (vrResponse?.playabilityStatus?.status == "OK") {
        println("$TAG: Successfully resolved player via ANDROID_VR")
        return@runCatchingNonCancellable vrResponse.applyDecipher(decipher)
    }
    logFailure("ANDROID_VR", vrResponse)
    
    // TIER 3: TVHTML5_SIMPLY_EMBEDDED_PLAYER (Permissive for embedded content)
    val tvResponse = tryPlayer(videoId, YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER, useCookies = false)
    if (tvResponse?.playabilityStatus?.status == "OK") {
        println("$TAG: Successfully resolved player via TVHTML5_SIMPLY_EMBEDDED_PLAYER")
        return@runCatchingNonCancellable tvResponse.applyDecipher(decipher)
    }
    logFailure("TVHTML5_SIMPLY_EMBEDDED_PLAYER", tvResponse)

    return@runCatchingNonCancellable tvResponse ?: vrResponse ?: webRemixResponse
}

private fun logFailure(clientName: String, response: PlayerResponse?) {
    val info = response?.playabilityStatus?.let { 
        "Status: ${it.status}, Reason: ${it.reason}, Messages: ${it.messages}"
    } ?: "Unknown error"
    println("$TAG: $clientName failed ($info)")
}

private suspend fun Innertube.tryPlayer(
    videoId: String, 
    clientType: YouTubeClient, 
    useCookies: Boolean,
    extraHeaders: Map<String, String> = emptyMap()
): PlayerResponse? = runCatching {
    println("$TAG: Attempting player request with client: ${clientType.clientName} (useCookies=$useCookies)")
    
    client.post(Innertube.PLAYER) {
        header("User-Agent", clientType.userAgent)
        
        // Control cookie usage via custom attribute
        attributes.put(Innertube.Attributes.UseCookies, useCookies)
        
        // Apply extra headers
        extraHeaders.forEach { (key, value) -> header(key, value) }
        
        setBody(
            PlayerBody(
                context = clientType.toContext(visitorData = visitorData),
                videoId = videoId,
                serviceIntegrityDimensions = poToken?.let { ServiceIntegrityDimensions(poToken = it) }
            )
        )
        mask("playabilityStatus(status,reason,messages),playerConfig.audioConfig,streamingData.adaptiveFormats,streamingData.formats,videoDetails.videoId")
    }.body<PlayerResponse>()
}.onFailure {
    println("$TAG: Network or parsing error for ${clientType.clientName}: ${it.message}")
}.getOrNull()

private suspend fun PlayerResponse.applyDecipher(decipher: (suspend (String) -> String)?): PlayerResponse {
    if (streamingData == null) return this
    
    return copy(
        streamingData = streamingData.copy(
            adaptiveFormats = streamingData.adaptiveFormats?.map { format ->
                val url = format.url ?: format.signatureCipher?.let { parseSignatureCipher(it, decipher) }
                format.copy(url = url?.let { decipherUrl(it, decipher) })
            },
            formats = streamingData.formats?.map { format ->
                val url = format.url ?: format.signatureCipher?.let { parseSignatureCipher(it, decipher) }
                format.copy(url = url?.let { decipherUrl(it, decipher) })
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
