package com.github.innertube.requests

import com.github.innertube.Innertube
import com.github.innertube.models.Context
import com.github.innertube.models.PlayerResponse
import com.github.innertube.models.YouTubeClient
import com.github.innertube.models.bodies.PlayerBody
import com.github.innertube.utils.runCatchingNonCancellable
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

fun PlayerResponse.isPlayable(): Boolean {
    return streamingData?.adaptiveFormats?.any { it.url != null } == true ||
            streamingData?.formats?.any { it.url != null } == true
}

suspend fun Innertube.player(videoId: String) = runCatchingNonCancellable {
    val token = visitorData().getOrNull()

    // 1. Try ANDROID_VR
    val androidVrResponse = client.post(PLAYER) {
        setBody(
            PlayerBody(
                context = YouTubeClient.ANDROID_VR.toContext(visitorData = token),
                videoId = videoId
            )
        )
        mask("playabilityStatus,playerConfig.audioConfig,streamingData,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (androidVrResponse.playabilityStatus?.status == "OK" && androidVrResponse.isPlayable()) {
        return@runCatchingNonCancellable androidVrResponse
    }
    
    println("SoundPod-InnerTube: ANDROID_VR failed for $videoId. Status: ${androidVrResponse.playabilityStatus?.status}, Reason: ${androidVrResponse.playabilityStatus?.reason}")

    // 2. Try TVHTML5_SIMPLY_EMBEDDED_PLAYER
    val tvResponse = client.post(PLAYER) {
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
        mask("playabilityStatus,playerConfig.audioConfig,streamingData,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (tvResponse.playabilityStatus?.status == "OK" && tvResponse.isPlayable()) {
        return@runCatchingNonCancellable tvResponse
    }
    
    println("SoundPod-InnerTube: TVHTML5 failed for $videoId. Status: ${tvResponse.playabilityStatus?.status}, Reason: ${tvResponse.playabilityStatus?.reason}")

    // 3. Try WEB_REMIX (YouTube Music Web)
    val webResponse = client.post(PLAYER) {
        setBody(
            PlayerBody(
                context = YouTubeClient.WEB_REMIX.toContext(visitorData = token),
                videoId = videoId
            )
        )
        mask("playabilityStatus,playerConfig.audioConfig,streamingData,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (webResponse.playabilityStatus?.status == "OK" && webResponse.isPlayable()) {
        return@runCatchingNonCancellable webResponse
    }
    
    println("SoundPod-InnerTube: WEB_REMIX failed for $videoId. Status: ${webResponse.playabilityStatus?.status}, Reason: ${webResponse.playabilityStatus?.reason}")

    // 4. Try WEB (YouTube Main Web)
    val webMainResponse = client.post(PLAYER) {
        setBody(
            PlayerBody(
                context = YouTubeClient.WEB.toContext(visitorData = token),
                videoId = videoId
            )
        )
        mask("playabilityStatus,playerConfig.audioConfig,streamingData,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (webMainResponse.playabilityStatus?.status == "OK" && webMainResponse.isPlayable()) {
        return@runCatchingNonCancellable webMainResponse
    }
    
    println("SoundPod-InnerTube: WEB failed for $videoId. Status: ${webMainResponse.playabilityStatus?.status}, Reason: ${webMainResponse.playabilityStatus?.reason}")

    // 5. Try ANDROID
    val androidResponse = client.post(PLAYER) {
        setBody(
            PlayerBody(
                context = YouTubeClient.ANDROID.toContext(visitorData = token),
                videoId = videoId
            )
        )
        mask("playabilityStatus,playerConfig.audioConfig,streamingData,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (androidResponse.playabilityStatus?.status == "OK" && androidResponse.isPlayable()) {
        return@runCatchingNonCancellable androidResponse
    }
    println("SoundPod-InnerTube: ANDROID failed for $videoId. Status: ${androidResponse.playabilityStatus?.status}, Reason: ${androidResponse.playabilityStatus?.reason}")

    // 6. Try MWEB
    val mwebResponse = client.post(PLAYER) {
        setBody(
            PlayerBody(
                context = YouTubeClient.MWEB.toContext(visitorData = token),
                videoId = videoId
            )
        )
        mask("playabilityStatus,playerConfig.audioConfig,streamingData,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (mwebResponse.playabilityStatus?.status == "OK" && mwebResponse.isPlayable()) {
        return@runCatchingNonCancellable mwebResponse
    }
    println("SoundPod-InnerTube: MWEB failed for $videoId. Status: ${mwebResponse.playabilityStatus?.status}, Reason: ${mwebResponse.playabilityStatus?.reason}")

    // 7. Try ANDROID_VR with forced US
    val androidVrUsResponse = client.post(PLAYER) {
        setBody(
            PlayerBody(
                context = YouTubeClient.ANDROID_VR.toContext(localized = false, visitorData = token),
                videoId = videoId
            )
        )
        mask("playabilityStatus,playerConfig.audioConfig,streamingData,videoDetails.videoId")
    }.body<PlayerResponse>()

    if (androidVrUsResponse.playabilityStatus?.status == "OK" && androidVrUsResponse.isPlayable()) {
        return@runCatchingNonCancellable androidVrUsResponse
    }
    println("SoundPod-InnerTube: ANDROID_VR_US failed for $videoId. Status: ${androidVrUsResponse.playabilityStatus?.status}, Reason: ${androidVrUsResponse.playabilityStatus?.reason}")

    // If all failed, return the first one (usually contains the error reason)
    return@runCatchingNonCancellable androidVrResponse
}
