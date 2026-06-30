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
    return playabilityStatus?.status == "OK" && (
            streamingData?.adaptiveFormats?.any { it.url != null } == true ||
            streamingData?.formats?.any { it.url != null } == true
    )
}

private data class PlayerStrategy(
    val client: YouTubeClient,
    val localized: Boolean = true,
    val isEmbedded: Boolean = false
)

private suspend fun Innertube.fetchPlayerRaw(
    videoId: String,
    strategy: PlayerStrategy,
    visitorData: String?
): PlayerResponse {
    val context = strategy.client.toContext(strategy.localized, visitorData).let {
        if (strategy.isEmbedded) {
            it.copy(thirdParty = Context.ThirdParty(embedUrl = "https://www.youtube.com/watch?v=$videoId"))
        } else it
    }
    return client.post(PLAYER) {
        setBody(PlayerBody(context = context, videoId = videoId))
        mask("playabilityStatus,playerConfig.audioConfig,streamingData,videoDetails.videoId")
    }.body()
}

suspend fun Innertube.player(videoId: String) = runCatchingNonCancellable {
    val token = visitorData().getOrNull()

    val strategies = listOf(
        PlayerStrategy(YouTubeClient.ANDROID_VR),
        PlayerStrategy(YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER, isEmbedded = true),
        PlayerStrategy(YouTubeClient.WEB_REMIX),
        PlayerStrategy(YouTubeClient.WEB),
        PlayerStrategy(YouTubeClient.ANDROID),
        PlayerStrategy(YouTubeClient.MWEB),
        PlayerStrategy(YouTubeClient.ANDROID_VR, localized = false)
    )

    var firstResponse: PlayerResponse? = null

    for (strategy in strategies) {
        val response = try {
            fetchPlayerRaw(videoId, strategy, token)
        } catch (e: Exception) {
            println("SoundPod-InnerTube: ${strategy.client.clientName} request failed for $videoId: ${e.message}")
            continue
        }

        if (firstResponse == null) firstResponse = response

        if (response.isPlayable()) {
            response.clientName = strategy.client.clientName
            return@runCatchingNonCancellable response
        }
        
        println("SoundPod-InnerTube: ${strategy.client.clientName} failed for $videoId. Status: ${response.playabilityStatus?.status}, Reason: ${response.playabilityStatus?.reason}")
    }

    return@runCatchingNonCancellable firstResponse ?: throw Exception("All player strategies failed")
}
