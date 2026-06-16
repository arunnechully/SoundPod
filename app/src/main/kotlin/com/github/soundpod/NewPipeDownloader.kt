package com.github.soundpod

import com.github.innertube.models.PlayerResponse
import java.util.concurrent.ConcurrentHashMap

class NewPipeDownloader private constructor() {
    private val playerResponseCache = ConcurrentHashMap<String, Pair<PlayerResponse, Long>>()

    fun preCache(videoId: String, playerResponse: PlayerResponse) {
        playerResponseCache[videoId] = playerResponse to System.currentTimeMillis()
    }

    companion object {
        private var instance: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader {
            if (instance == null) {
                instance = NewPipeDownloader()
            }
            return instance!!
        }
    }
}
