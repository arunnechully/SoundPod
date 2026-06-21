package com.github.soundpod.extractor.youtube

class YoutubeService {
    fun getStreamExtractor(url: String): YoutubeStreamExtractor {
        return YoutubeStreamExtractor(url)
    }
}
