package com.github.soundpod.service

import com.github.innertube.Innertube
import com.github.innertube.requests.lyrics
import com.github.soundpod.db
import com.github.soundpod.models.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.database.sqlite.SQLiteConstraintException

object LyricsFetcher {

    suspend fun fetchLyrics(
        mediaId: String
    ): Boolean = withContext(Dispatchers.IO) {

        // Fetch unsynced (fixed) lyrics from InnerTube
        var fixedLyrics: String? = null
        Innertube.lyrics(videoId = mediaId)?.onSuccess { fixedLyrics = it }

        // If fetch succeeds, update the database
        if (!fixedLyrics.isNullOrBlank()) {
            try {
                db.upsert(
                    Lyrics(
                        songId = mediaId,
                        fixed = fixedLyrics,
                        synced = null
                    )
                )
                return@withContext true
            } catch (_: SQLiteConstraintException) {
                return@withContext false
            }
        }

        return@withContext false
    }
}