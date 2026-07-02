package com.github.soundpod.extractor.youtube

import com.github.innertube.models.PlayerResponse
import com.github.soundpod.extractor.NewPipeHelper
import com.github.soundpod.service.YouTubeDecipherer
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.Stream as NewPipeStream

class YoutubeStreamExtractor(
    private val url: String,
    private val playerResult: PlayerResponse? = null
) {
    private var realExtractor: org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor? = null
    private var playerResponse: PlayerResponse? = playerResult
    
    private var cachedAudioStreams: List<Stream>? = null
    private var cachedVideoStreams: List<Stream>? = null

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
            return audioStreams.minByOrNull { it.averageBitrate }
        }

    val audioStreams: List<Stream>
        get() {
            cachedAudioStreams?.let { return it }
            
            val streams = if (realExtractor != null) {
                val ext = realExtractor!!
                val audioOnly = ext.audioStreams.map { RealStreamWrapper(it) }
                if (audioOnly.isNotEmpty()) audioOnly else ext.videoStreams.map { RealStreamWrapper(it) }
            } else if (playerResponse?.streamingData != null) {
                val streamingData = playerResponse!!.streamingData!!
                val combined = (streamingData.adaptiveFormats.orEmpty() + streamingData.formats.orEmpty())
                    .filter { it.mimeType.contains("audio", ignoreCase = true) || it.audioQuality != null || it.mimeType.contains("mp4a") }
                decipherStreams(combined).map { AudioStream(it) }.filter { it.content.isNotBlank() }
            } else {
                emptyList()
            }
            
            cachedAudioStreams = streams
            return streams
        }

    val videoStreams: List<Stream>
        get() {
            cachedVideoStreams?.let { return it }
            
            val streams = if (realExtractor != null) {
                realExtractor!!.videoStreams.map { RealStreamWrapper(it) }
            } else if (playerResponse?.streamingData != null) {
                val streamingData = playerResponse!!.streamingData!!
                val combined = (streamingData.adaptiveFormats.orEmpty() + streamingData.formats.orEmpty())
                    .filter { it.mimeType.startsWith("video/") }
                decipherStreams(combined).map { VideoStream(it) }.filter { it.content.isNotBlank() }
            } else {
                emptyList()
            }
            
            cachedVideoStreams = streams
            return streams
        }

    private fun decipherStreams(formats: List<PlayerResponse.StreamingData.AdaptiveFormat>): List<PlayerResponse.StreamingData.AdaptiveFormat> {
        val cipherFormats = formats.filter { it.url == null && it.signatureCipher != null }
        if (cipherFormats.isEmpty()) return formats
        
        val cipherData = cipherFormats.map { format ->
            format.signatureCipher!!.split("&").associate {
                val pair = it.split("=")
                pair[0] to java.net.URLDecoder.decode(pair.getOrNull(1) ?: "", "UTF-8")
            }
        }
        
        val sList = cipherData.map { it["s"] ?: "" }
        val decipheredSList = runBlocking { YouTubeDecipherer.signatureDecipher(sList) }
        
        val decipheredMap = cipherFormats.mapIndexed { index, format ->
            val data = cipherData[index]
            val baseUrl = data["url"] ?: ""
            val s = decipheredSList[index]
            val sp = data["sp"] ?: "sig"
            
            if (baseUrl.isNotBlank() && s.isNotBlank()) {
                val finalUrl = if (baseUrl.contains("?")) "$baseUrl&$sp=$s" else "$baseUrl?$sp=$s"
                format.copy(url = finalUrl)
            } else format
        }.associateBy { it.itag }

        return formats.map { format ->
            if (format.url != null) format else decipheredMap[format.itag] ?: format
        }
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
