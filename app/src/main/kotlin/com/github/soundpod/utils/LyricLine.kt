package com.github.soundpod.utils

data class LyricLine(
    val text: String,
    val startTime: Long,
)

sealed interface LyricsData {
    data class Synced(val lines: List<LyricLine>) : LyricsData
    data class Unsynced(val text: String) : LyricsData
    data object None : LyricsData
}

object LyricsParser {

    fun parse(syncedLyrics: String?, fixedLyrics: String?): LyricsData {
        if (!syncedLyrics.isNullOrBlank()) {
            val lines = parseLrc(syncedLyrics)
            if (lines.isNotEmpty()) {
                return LyricsData.Synced(lines)
            }
        }

        if (!fixedLyrics.isNullOrBlank()) {
            return LyricsData.Unsynced(fixedLyrics)
        }

        return LyricsData.None
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d+):(\\d+\\.?\\d*)\\](.*)")
        
        lrc.lines().forEach { line ->
            regex.find(line)?.let { matchResult ->
                val minutes = matchResult.groupValues[1].toLong()
                val seconds = matchResult.groupValues[2].toDouble()
                val text = matchResult.groupValues[3].trim()
                
                val startTime = (minutes * 60 * 1000 + seconds * 1000).toLong()
                lines.add(LyricLine(text, startTime))
            }
        }
        
        return lines.sortedBy { it.startTime }
    }
}
