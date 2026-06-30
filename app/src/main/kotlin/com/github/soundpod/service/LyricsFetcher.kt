package com.github.soundpod.service

import android.database.sqlite.SQLiteConstraintException
import com.github.innertube.Innertube
import com.github.innertube.requests.lyrics
import com.github.soundpod.db
import com.github.soundpod.models.Lyrics
import com.github.betterlyrics.BetterLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

object LyricsFetcher {

    suspend fun fetchLyrics(
        mediaId: String,
        title: String? = null,
        artist: String? = null,
        duration: Int? = null,
        album: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val song = if (title == null || artist == null) {
            db.song(mediaId).firstOrNull()
        } else null
        
        val finalTitle = title ?: song?.title ?: return@withContext false
        val finalArtist = artist ?: song?.artistsText ?: ""
        val finalAlbum = album ?: db.songAlbumInfo(mediaId)?.name
        
        val finalDuration = duration ?: song?.durationText?.split(":")?.let { parts ->
            if (parts.size == 2) {
                parts[0].toInt() * 60 + parts[1].toInt()
            } else if (parts.size == 3) {
                parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
            } else null
        } ?: -1

        var syncedLyrics: String? = null
        
        // Try to fetch synced lyrics from BetterLyrics
        BetterLyrics.getLyrics(
            title = finalTitle,
            artist = finalArtist,
            duration = finalDuration,
            album = finalAlbum
        ).onSuccess { lyrics: String ->
            syncedLyrics = lyrics
        }.onFailure { e: Throwable ->
            Timber.e(e, "Failed to fetch synced lyrics for $mediaId")
        }

        // Fetch unsynced (fixed) lyrics from InnerTube
        var fixedLyrics: String? = null
        Innertube.lyrics(videoId = mediaId)?.onSuccess { fixedLyrics = it }

        // If fetch succeeds (either synced or fixed), update the database
        if (!syncedLyrics.isNullOrBlank() || !fixedLyrics.isNullOrBlank()) {
            try {
                db.upsert(
                    Lyrics(
                        songId = mediaId,
                        fixed = fixedLyrics,
                        synced = syncedLyrics
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
