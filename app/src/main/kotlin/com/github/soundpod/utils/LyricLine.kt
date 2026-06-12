package com.github.soundpod.utils

sealed interface LyricsData {
    data class Unsynced(val text: String) : LyricsData
    data object None : LyricsData
}

object LyricsParser {

    fun parse(syncedLyrics: String?, fixedLyrics: String?): LyricsData {
        if (!fixedLyrics.isNullOrBlank()) {
            return LyricsData.Unsynced(fixedLyrics)
        }

        return LyricsData.None
    }
}
