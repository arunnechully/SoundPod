package com.github.innertube.models

import com.github.innertube.BotGuardData
import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus?,
    val playerConfig: PlayerConfig?,
    val streamingData: StreamingData?,
    val videoDetails: VideoDetails?,
) {
    @Serializable
    data class PlayabilityStatus(
        val status: String?,
        val reason: String? = null,
        val botguardData: BotGuardData? = null
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig?
    ) {
        @Serializable
        data class AudioConfig(
            private val loudnessDb: Double?,
            private val perceptualLoudnessDb: Double?
        ) {
            val normalizedLoudnessDb: Float?
                get() = (loudnessDb ?: perceptualLoudnessDb)?.plus(7)?.toFloat()
        }
    }

    @Serializable
    data class StreamingData(
        val adaptiveFormats: List<AdaptiveFormat>?,
        val formats: List<AdaptiveFormat>?,
    ) {
        val highestQualityFormat: AdaptiveFormat?
            get() = (adaptiveFormats.orEmpty() + formats.orEmpty())
                .filter { it.mimeType.contains("audio") && it.url != null }
                .maxByOrNull {
                    when (it.itag) {
                        251 -> 1000000L
                        140 -> 900000L
                        else -> it.bitrate ?: 0L
                    }
                }

        @Serializable
        data class AdaptiveFormat(
            val itag: Int,
            val mimeType: String,
            val bitrate: Long?,
            val averageBitrate: Long?,
            val contentLength: Long?,
            val audioQuality: String?,
            val approxDurationMs: Long?,
            val lastModified: Long?,
            val loudnessDb: Double?,
            val audioSampleRate: Int?,
            val url: String?,
        )
    }

    @Serializable
    data class VideoDetails(
        val videoId: String?,
        val author: String? = null,
        val thumbnail: ThumbnailRenderer.MusicThumbnailRenderer.Thumbnail? = null
    )
}
