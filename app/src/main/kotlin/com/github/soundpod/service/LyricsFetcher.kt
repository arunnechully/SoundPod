package com.github.soundpod.service

import androidx.media3.common.MediaMetadata
import com.github.innertube.Innertube
import com.github.innertube.requests.lyrics
import com.github.kugou.KuGou
import com.github.soundpod.db
import com.github.soundpod.models.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
// Import the SQLite exception
import android.database.sqlite.SQLiteConstraintException

object LyricsFetcher {

    suspend fun fetchLyrics(
        mediaId: String,
        mediaMetadata: MediaMetadata,
        durationMs: Long
    ): Boolean = withContext(Dispatchers.IO) {

        val fixedDeferred = async {
            var fixedResult: String? = null
            Innertube.lyrics(videoId = mediaId)?.onSuccess { fixedResult = it }
            fixedResult
        }

        val syncedDeferred = async {
            val gitHubLyrics = fetchFromGitHub(mediaId)
            if (!gitHubLyrics.isNullOrBlank()) {
                return@async gitHubLyrics
            }

            var kugouResult: String? = null
            KuGou.lyrics(
                artist = mediaMetadata.artist?.toString() ?: "",
                title = mediaMetadata.title?.toString() ?: "",
                duration = durationMs / 1000
            )?.onSuccess { syncedLyrics ->
                kugouResult = syncedLyrics?.value
            }
            kugouResult
        }

        val fixedLyrics = fixedDeferred.await()
        val syncedLyrics = syncedDeferred.await()

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
            val url = URL("https://raw.githubusercontent.com/arunnechully/SoundPod-LRC/main/lyrics/$mediaId.lrc")
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