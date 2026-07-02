package com.github.soundpod.extractor.youtube

import com.github.innertube.models.PlayerResponse

class YoutubeService {
    fun getStreamExtractor(url: String, playerResult: PlayerResponse? = null): YoutubeStreamExtractor {
        return YoutubeStreamExtractor(url, playerResult)
    }
}
