package com.github.soundpod.extractor.youtube

import com.github.innertube.models.PlayerResponse
import com.github.soundpod.extractor.NewPipeHelper
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.Stream as NewPipeStream

class YoutubeStreamExtractor(
    private val url: String,
    private val playerResult: PlayerResponse? = null
) {
    private var realExtractor: org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor? = null
    private var playerResponse: PlayerResponse? = playerResult

    fun fetchPage() {
        if (playerResponse != null) {
            val streamingData = playerResponse?.streamingData
            val allFormats = (streamingData?.adaptiveFormats.orEmpty() + streamingData?.formats.orEmpty())
            val hasStreams = allFormats.isNotEmpty()
            
            if (!hasStreams) {
                println("YoutubeStreamExtractor: PlayerResult provided but streamingData is empty for $url (Status: ${playerResponse?.playabilityStatus?.status}, Reason: ${playerResponse?.playabilityStatus?.reason}). Falling back to NewPipe.")
                playerResponse = null
            } else {
                val formatsWithUrl = allFormats.filter { it.url != null }
                val formatsWithCipher = allFormats.filter { it.signatureCipher != null }
                
                formatsWithUrl.forEach { 
                    println("YoutubeStreamExtractor: Available stream for $url: itag=${it.itag}, mime=${it.mimeType}, quality=${it.audioQuality}")
                }
                
                println("YoutubeStreamExtractor: Info for $url: total=${allFormats.size}, withUrl=${formatsWithUrl.size}, withCipher=${formatsWithCipher.size}")

                val availableAudio = audioStreams.filter { it.content.isNotBlank() }
                if (availableAudio.isEmpty()) {
                    println("YoutubeStreamExtractor: PlayerResult provided but no audio streams with URLs found for $url. Falling back to NewPipe.")
                    playerResponse = null
                } else {
                    println("YoutubeStreamExtractor: Found ${availableAudio.size} playable audio streams for $url")
                    return
                }
            }
        }

        try {
            NewPipeHelper.init()
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()
            realExtractor = extractor as? org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
        } catch (_: Exception) {
            // Silently fail
        }
    }

    val lowestQualityAudioStream: Stream?
        get() {
            realExtractor?.let { ext ->
                val audioOnly = ext.audioStreams.map { RealStreamWrapper(it) }
                if (audioOnly.isNotEmpty()) return audioOnly.minByOrNull { it.averageBitrate }
                return ext.videoStreams.map { RealStreamWrapper(it) }.minByOrNull { it.averageBitrate }
            }
            
            val streamingData = playerResponse?.streamingData ?: return null
            val combined = (streamingData.adaptiveFormats.orEmpty() + streamingData.formats.orEmpty())
                .filter { it.url != null }
            
            val audioOnly = combined.filter { it.mimeType.startsWith("audio/") }
            if (audioOnly.isNotEmpty()) return audioOnly.minByOrNull { it.averageBitrate ?: it.bitrate ?: Long.MAX_VALUE }?.let { AudioStream(it) }

            return combined
                .filter { it.mimeType.contains("audio", ignoreCase = true) || it.audioQuality != null || it.mimeType.contains("mp4a") }
                .minByOrNull { it.averageBitrate ?: it.bitrate ?: Long.MAX_VALUE }
                ?.let { AudioStream(it) }
        }

    val audioStreams: List<Stream>
        get() {
            realExtractor?.let { ext ->
                val audioOnly = ext.audioStreams.map { RealStreamWrapper(it) }
                if (audioOnly.isNotEmpty()) return audioOnly
                
                // Fallback to video streams (muxed) if audio-only is missing
                return ext.videoStreams.map { RealStreamWrapper(it) }
            }
            
            val streamingData = playerResponse?.streamingData ?: return emptyList()
            val combined = streamingData.adaptiveFormats.orEmpty() + streamingData.formats.orEmpty()
            
            // Prioritize streams that actually have a URL
            val allAudio = combined
                .filter { it.mimeType.contains("audio", ignoreCase = true) || it.audioQuality != null || it.mimeType.contains("mp4a") }
            
            val withUrl = allAudio.filter { it.url != null }
            if (withUrl.isNotEmpty()) return withUrl.map { AudioStream(it) }

            // If none have URLs, return all (might be encrypted)
            return allAudio.map { AudioStream(it) }
        }

    val videoStreams: List<Stream>
        get() {
            realExtractor?.let { ext ->
                return ext.videoStreams.map { RealStreamWrapper(it) }
            }
            
            val streamingData = playerResponse?.streamingData ?: return emptyList()
            val combined = streamingData.adaptiveFormats.orEmpty() + streamingData.formats.orEmpty()
            
            return combined
                .filter { it.mimeType.startsWith("video/") }
                .map { VideoStream(it) }
        }

    interface Stream {
        val content: String
        val codec: String?
        val averageBitrate: Long
        val contentLength: Long?
        val bitrate: Long get() = averageBitrate
    }

    class AudioStream(private val format: PlayerResponse.StreamingData.AdaptiveFormat) : Stream {
        override val codec: String get() = format.mimeType.substringAfter("codecs=\"").substringBefore("\"")
        override val averageBitrate: Long get() = format.averageBitrate ?: format.bitrate ?: 0L
        override val contentLength: Long? get() = format.contentLength
        override val content: String get() = format.url ?: ""
    }

    class VideoStream(private val format: PlayerResponse.StreamingData.AdaptiveFormat) : Stream {
        override val codec: String? get() = null
        override val averageBitrate: Long get() = format.bitrate ?: 0L
        override val contentLength: Long? get() = format.contentLength
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
                val bit = stream.bitrate.toLong()
                // NewPipe bitrates can sometimes be in kbps or bps depending on extractor version
                // For YouTube it's usually bps, but we normalize here.
                val base = if (avg > 0) avg else bit
                if (base in 1..<1000) base * 1000 else base
            }
            is org.schabi.newpipe.extractor.stream.VideoStream -> {
                val bit = stream.bitrate.toLong()
                if (bit in 1..<1000) bit * 1000 else bit
            }
            else -> 0L
        }
        override val contentLength: Long? get() = null // NewPipe doesn't always provide this upfront
    }
}
