package org.schabi.newpipe.extractor.services.youtube

import com.github.innertube.Innertube
import com.github.innertube.models.PlayerResponse
import com.github.innertube.requests.player
import com.github.soundpod.extractor.NewPipeHelper
import kotlinx.coroutines.runBlocking

class YoutubeStreamExtractor(private val url: String) {
    private val videoId = url.substringAfter("v=").substringBefore("&")
    private var playerResponse: PlayerResponse? = null
    private var realExtractor: Any? = null

    fun fetchPage() {
        // PRIMARY: Try Innertube (which uses WebView session)
        playerResponse = runBlocking { Innertube.player(videoId) }?.getOrNull()
        
        // SECONDARY: If Innertube failed or returned unplayable, and library is available, try real NewPipe
        if ((playerResponse == null || playerResponse?.playabilityStatus?.status != "OK") && NewPipeHelper.isLibraryAvailable) {
            try {
                NewPipeHelper.init()
                val serviceListClass = Class.forName("org.schabi.newpipe.extractor.ServiceList")
                val youtubeService = serviceListClass.getField("YouTube").get(null)
                val getStreamExtractorMethod = youtubeService.javaClass.getMethod("getStreamExtractor", String::class.java)
                val extractor = getStreamExtractorMethod.invoke(youtubeService, url)
                
                extractor.javaClass.getMethod("fetchPage").invoke(extractor)
                realExtractor = extractor
                println("SoundPod-Extractor: Switched to real NewPipe extraction for $videoId")
            } catch (e: Exception) {
                println("SoundPod-Extractor: Failed to use real NewPipe fallback: ${e.message}")
            }
        }
    }

    val audioStreams: List<Stream>
        get() {
            if (realExtractor != null) {
                return (realExtractor!!.javaClass.getMethod("getAudioStreams").invoke(realExtractor) as List<*>).map { RealStreamWrapper(it!!) }
            }
            return playerResponse?.streamingData?.adaptiveFormats
                ?.filter { it.mimeType.startsWith("audio/") }
                ?.map { AudioStream(it) } ?: emptyList()
        }

    val videoStreams: List<Stream>
        get() {
            if (realExtractor != null) {
                return (realExtractor!!.javaClass.getMethod("getVideoStreams").invoke(realExtractor) as List<*>).map { RealStreamWrapper(it!!) }
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
    
    class RealStreamWrapper(private val realStream: Any) : Stream {
        override val content: String get() = realStream.javaClass.getMethod("getContent").invoke(realStream) as String
        override val codec: String? get() = try { realStream.javaClass.getMethod("getCodec").invoke(realStream) as String } catch(e: Exception) { null }
        override val averageBitrate: Long get() = try { (realStream.javaClass.getMethod("getAverageBitrate").invoke(realStream) as Number).toLong() } catch(e: Exception) { 
            try { (realStream.javaClass.getMethod("getBitrate").invoke(realStream) as Number).toLong() } catch(e2: Exception) { 0L }
        }
    }
}
