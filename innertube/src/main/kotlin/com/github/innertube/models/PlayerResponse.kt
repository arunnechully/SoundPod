package com.github.innertube.models

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
        val reason: String?,
        val messages: List<String>? = null
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig?
    ) {
        @Serializable
        data class AudioConfig(
            private val loudnessDb: Double? = null,
            private val perceptualLoudnessDb: Double? = null
        ) {
            // For music clients only
            val normalizedLoudnessDb: Float?
                get() = (loudnessDb ?: perceptualLoudnessDb)?.plus(7)?.toFloat()
        }
    }

    @Serializable
    data class StreamingData(
        val adaptiveFormats: List<AdaptiveFormat>?,
        val formats: List<AdaptiveFormat>? = null
    ) {
        val highestQualityFormat: AdaptiveFormat?
            get() {
                val allFormats = (adaptiveFormats.orEmpty() + formats.orEmpty())
                    .filter { it.url != null }
                
                return allFormats.find { it.itag == 251 }
                    ?: allFormats.find { it.itag == 140 }
                    ?: allFormats.filter { it.mimeType.startsWith("audio/") }.maxByOrNull { it.bitrate ?: 0L }
                    ?: allFormats.maxByOrNull { it.bitrate ?: 0L }
            }

        @Serializable
        data class AdaptiveFormat(
            val itag: Int,
            val mimeType: String,
            val bitrate: Long?,
            val averageBitrate: Long? = null,
            val contentLength: Long? = null,
            val audioQuality: String? = null,
            val approxDurationMs: Long? = null,
            val lastModified: Long? = null,
            val loudnessDb: Double? = null,
            val audioSampleRate: Int? = null,
            val url: String? = null,
            val signatureCipher: String? = null,
        )
    }

    @Serializable
    data class VideoDetails(
        val videoId: String?
    )
}
