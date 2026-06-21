package org.schabi.newpipe.extractor.services.youtube

class YoutubeService {
    fun getStreamExtractor(url: String): YoutubeStreamExtractor {
        return YoutubeStreamExtractor(url)
    }
}
