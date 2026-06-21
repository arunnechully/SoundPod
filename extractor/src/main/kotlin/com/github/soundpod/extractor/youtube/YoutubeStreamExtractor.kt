package com.github.soundpod.extractor.youtube

import com.github.innertube.Innertube
import com.github.innertube.models.PlayerResponse
import com.github.innertube.requests.player
import com.github.soundpod.extractor.NewPipeHelper
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.Stream as NewPipeStream

class YoutubeStreamExtractor(private val url: String) {
    private val videoId = url.substringAfter("v=").substringBefore("&")
    private var playerResponse: PlayerResponse? = null
    private var realExtractor: org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor? = null

    fun fetchPage() {
        // PRIMARY: Try Innertube
        playerResponse = runBlocking { Innertube.player(videoId) }?.getOrNull()
        
        // SECONDARY: Fallback to real NewPipe (now non-optional as per user request)
        if (playerResponse == null || playerResponse?.playabilityStatus?.status != "OK") {
            try {
                NewPipeHelper.init()
                val extractor = ServiceList.YouTube.getStreamExtractor(url)
                extractor.fetchPage()
                realExtractor = extractor as? org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
                println("SoundPod-Extractor: Switched to real NewPipe extraction for $videoId")
            } catch (e: Exception) {
                println("SoundPod-Extractor: Failed to use real NewPipe fallback: ${e.message}")
            }
        }
    }

    val audioStreams: List<Stream>
        get() {
            realExtractor?.let { ext ->
                return ext.audioStreams.map { RealStreamWrapper(it) }
            }
            return playerResponse?.streamingData?.adaptiveFormats
                ?.filter { it.mimeType.startsWith("audio/") }
                ?.map { AudioStream(it) } ?: emptyList()
        }

    val videoStreams: List<Stream>
        get() {
            realExtractor?.let { ext ->
                return ext.videoStreams.map { RealStreamWrapper(it) }
            }
            return playerResponse?.streamingData?.adaptiveFormats
                ?.filter { it.mimeType.startsWith("video/") }
                ?.map { VideoStream(it) } ?: emptyList()
        }

    interface Stream {
        val content: String
        val codec: String?
        val averageBitrate: Long
        val bitrate: Long get() = averageBitrate
    }

    class AudioStream(private val format: PlayerResponse.StreamingData.AdaptiveFormat) : Stream {
        override val codec: String get() = format.mimeType.substringAfter("codecs=\"").substringBefore("\"")
        override val averageBitrate: Long get() = format.averageBitrate ?: format.bitrate ?: 0L
        override val content: String get() = format.url ?: ""
    }

    class VideoStream(private val format: PlayerResponse.StreamingData.AdaptiveFormat) : Stream {
        override val codec: String? get() = null
        override val averageBitrate: Long get() = format.bitrate ?: 0L
        override val content: String get() = format.url ?: ""
    }
    
    class RealStreamWrapper(private val realStream: NewPipeStream) : Stream {
        override val content: String get() = realStream.content
        override val codec: String? get() = when (realStream) {
            is org.schabi.newpipe.extractor.stream.AudioStream -> realStream.codec
            is org.schabi.newpipe.extractor.stream.VideoStream -> realStream.codec
            else -> null
        }
        override val averageBitrate: Long get() = when (val stream = realStream) {
            is org.schabi.newpipe.extractor.stream.AudioStream -> {
                val avg = stream.averageBitrate.toLong()
                if (avg > 0) avg else stream.bitrate.toLong()
            }
            is org.schabi.newpipe.extractor.stream.VideoStream -> stream.bitrate.toLong()
            else -> 0L
        }
    }
}
