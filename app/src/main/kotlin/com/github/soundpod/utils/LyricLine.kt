package com.github.soundpod.utils

data class LyricLine(
    val startMs: Long,
    val text: String
)

sealed interface LyricsData {
    data class Synced(val lines: List<LyricLine>) : LyricsData
    data class Unsynced(val text: String) : LyricsData
    data object None : LyricsData
}

object LyricsParser {

    private val timeStampRegex = "\\[(\\d{2,}):(\\d{2})(?:\\.(\\d{1,3}))?]".toRegex()

    fun parse(syncedLyrics: String?, fixedLyrics: String?): LyricsData {
        if (!syncedLyrics.isNullOrBlank()) {
            val parsedLines = parseLrc(syncedLyrics)
            if (parsedLines.isNotEmpty()) {
                return LyricsData.Synced(parsedLines)
            }
        }

        if (!fixedLyrics.isNullOrBlank()) {
            return LyricsData.Unsynced(fixedLyrics)
        }

        return LyricsData.None
    }

    private fun parseLrc(lrcString: String): List<LyricLine> {
        val lines = lrcString.lines()
        val result = mutableListOf<LyricLine>()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val matches = timeStampRegex.findAll(trimmedLine).toList()
            if (matches.isEmpty()) continue

            val text = timeStampRegex.replace(trimmedLine, "").trim()

            for (match in matches) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msStr = match.groupValues[3]

                val ms = if (msStr.isEmpty()) {
                    0L
                } else {
                    msStr.padEnd(3, '0').toLong()
                }

                val timeMs = (min * 60 * 1000) + (sec * 1000) + ms
                result.add(LyricLine(startMs = timeMs, text = text))
            }
        }

        return result.sortedBy { it.startMs }
    }
}