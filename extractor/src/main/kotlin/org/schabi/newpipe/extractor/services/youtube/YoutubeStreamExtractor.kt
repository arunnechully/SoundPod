package org.schabi.newpipe.extractor.services.youtube

import com.github.innertube.Innertube
import com.github.innertube.models.PlayerResponse
import com.github.innertube.requests.player
import kotlinx.coroutines.runBlocking

class YoutubeStreamExtractor(private val videoId: String) {
    private var playerResponse: PlayerResponse? = null

    fun fetchPage() {
        playerResponse = runBlocking { Innertube.player(videoId) }?.getOrNull()
    }

    val audioStreams: List<AudioStream>
        get() = playerResponse?.streamingData?.adaptiveFormats
            ?.filter { it.mimeType.startsWith("audio/") }
            ?.map { AudioStream(it) } ?: emptyList()

    val videoStreams: List<VideoStream>
        get() = playerResponse?.streamingData?.adaptiveFormats
            ?.filter { it.mimeType.startsWith("video/") }
            ?.map { VideoStream(it) } ?: emptyList()

    interface Stream {
        val content: String
    }

    class AudioStream(private val format: PlayerResponse.StreamingData.AdaptiveFormat) : Stream {
        val codec: String get() = format.mimeType.substringAfter("codecs=\"").substringBefore("\"")
        val averageBitrate: Long get() = format.averageBitrate ?: format.bitrate ?: 0L
        override val content: String get() = format.url ?: ""
    }

    class VideoStream(private val format: PlayerResponse.StreamingData.AdaptiveFormat) : Stream {
        val bitrate: Long get() = format.bitrate ?: 0L
        override val content: String get() = format.url ?: ""
    }
}
