package com.github.soundpod.service

import com.github.innertube.Innertube
import com.github.innertube.requests.lyrics
import com.github.soundpod.db
import com.github.soundpod.models.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import android.database.sqlite.SQLiteConstraintException

object LyricsFetcher {

    suspend fun fetchLyrics(
        mediaId: String
    ): Boolean = withContext(Dispatchers.IO) {

        // Fetch unsynced (fixed) lyrics from InnerTube
        val fixedDeferred = async {
            var fixedResult: String? = null
            Innertube.lyrics(videoId = mediaId)?.onSuccess { fixedResult = it }
            fixedResult
        }

        // Fetch synced lyrics from your GitHub repository
        val syncedDeferred = async {
            fetchFromGitHub(mediaId)
        }

        val fixedLyrics = fixedDeferred.await()
        val syncedLyrics = syncedDeferred.await()

        // If either fetch succeeds, update the database
        if (!fixedLyrics.isNullOrBlank() || !syncedLyrics.isNullOrBlank()) {
            val currentLyrics = db.lyrics(mediaId).firstOrNull()
            try {
                db.upsert(
                    Lyrics(
                        songId = mediaId,
                        fixed = fixedLyrics ?: currentLyrics?.fixed,
                        synced = syncedLyrics ?: currentLyrics?.synced
                    )
                )
                return@withContext true
            } catch (_: SQLiteConstraintException) {
                return@withContext false
            }
        }

        return@withContext false
    }

    private fun fetchFromGitHub(mediaId: String): String? {
        return try {
            val cdnUrl = "https://cdn.jsdelivr.net/gh/arunnechully/SoundPod-LRC@main/lyrics/$mediaId.lrc"
            val url = URL(cdnUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}