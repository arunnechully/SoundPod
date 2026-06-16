package org.schabi.newpipe.extractor.services.youtube

class YoutubeService {
    fun getStreamExtractor(url: String): YoutubeStreamExtractor {
        val videoId = url.substringAfter("v=").substringBefore("&")
        return YoutubeStreamExtractor(videoId)
    }
}
