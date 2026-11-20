package com.github.soundpod.service

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import androidx.core.net.toUri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class JioSaavnSong(
    val id: String,
    val title: String,
    val artistsText: String?,
    val durationText: String?,
    val thumbnailUrl: String?,
    val streamUrl: String?,
    val albumTitle: String?
)

data class JioSaavnAlbum(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val songs: List<JioSaavnSong>
)

@Suppress("unused")
object JioSaavn {
    private val client = OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

    fun decryptUrl(encryptedUrl: String): String {
        return try {
            @SuppressLint("GetInstance")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            val key = SecretKeySpec("38346591".toByteArray(Charsets.UTF_8), "DES")
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            var url = String(decryptedBytes, Charsets.UTF_8)
            url = url.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4")
            url
        } catch (e: Exception) {
            Log.e("SoundPod-JioSaavn", "Failed to decrypt URL: $encryptedUrl", e)
            encryptedUrl
        }
    }

    fun searchSongs(query: String): List<JioSaavnSong> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?_format=json&_marker=0&api_version=4&ctx=web6dot0&n=20&p=1&q=$encodedQuery&__call=search.getResults"
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: JSONArray()
                val list = mutableListOf<JioSaavnSong>()
                for (i in 0 until results.length()) {
                    val obj = results.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: obj.optString("perma_url")
                    val title = obj.optString("title").takeIf { it.isNotBlank() } ?: obj.optString("song")
                    val moreInfo = obj.optJSONObject("more_info")
                    val primaryArtists = moreInfo?.optString("primary_artists") ?: obj.optString("primary_artists")
                    val singers = moreInfo?.optString("singers") ?: primaryArtists
                    val duration = moreInfo?.optString("duration")
                    val durationText = duration?.toLongOrNull()?.let {
                        val min = it / 60
                        val sec = it % 60
                        String.format(Locale.ROOT, "%d:%02d", min, sec)
                    } ?: "3:30"

                    val image = obj.optString("image").replace("150x150", "500x500")
                    val encUrl = moreInfo?.optString("encrypted_media_url") ?: obj.optString("encrypted_media_url")
                    val streamUrl = if (!encUrl.isNullOrBlank()) decryptUrl(encUrl) else (moreInfo?.optString("media_url") ?: obj.optString("media_url"))
                    val albumTitle = moreInfo?.optString("album")

                    list.add(
                        JioSaavnSong(
                            id = id,
                            title = title,
                            artistsText = singers.ifBlank { null },
                            durationText = durationText,
                            thumbnailUrl = image.ifBlank { null },
                            streamUrl = streamUrl.ifBlank { null },
                            albumTitle = albumTitle?.ifBlank { null }
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e("SoundPod-JioSaavn", "Search failed for $query", e)
            emptyList()
        }
    }

    fun getAlbumDetails(albumId: String): JioSaavnAlbum? {
        return try {
            val url = "https://www.jiosaavn.com/api.php?__call=content.getAlbumDetails&albumid=$albumId&_format=json&_marker=0&api_version=4&ctx=web6dot0"
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                val json = JSONObject(body)
                val title = json.optString("title").takeIf { it.isNotBlank() } ?: json.optString("name")
                val image = json.optString("image").replace("150x150", "500x500")
                val songsArray = json.optJSONArray("songs") ?: JSONArray()
                val songs = mutableListOf<JioSaavnSong>()
                for (i in 0 until songsArray.length()) {
                    val obj = songsArray.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    val songTitle = obj.optString("title").takeIf { it.isNotBlank() } ?: obj.optString("song")
                    val moreInfo = obj.optJSONObject("more_info")
                    val primaryArtists = moreInfo?.optString("primary_artists") ?: obj.optString("primary_artists")
                    val duration = moreInfo?.optString("duration")
                    val durationText = duration?.toLongOrNull()?.let {
                        val min = it / 60
                        val sec = it % 60
                        String.format(Locale.ROOT, "%d:%02d", min, sec)
                    } ?: "3:30"
                    val songImage = (obj.optString("image").takeIf { it.isNotBlank() } ?: image).replace("150x150", "500x500")
                    val encUrl = moreInfo?.optString("encrypted_media_url") ?: obj.optString("encrypted_media_url")
                    val streamUrl = if (!encUrl.isNullOrBlank()) decryptUrl(encUrl) else (moreInfo?.optString("media_url") ?: obj.optString("media_url"))

                    songs.add(
                        JioSaavnSong(
                            id = id,
                            title = songTitle,
                            artistsText = primaryArtists.ifBlank { null },
                            durationText = durationText,
                            thumbnailUrl = songImage.ifBlank { null },
                            streamUrl = streamUrl.ifBlank { null },
                            albumTitle = title.ifBlank { null }
                        )
                    )
                }
                JioSaavnAlbum(
                    id = albumId,
                    title = title,
                    thumbnailUrl = image.ifBlank { null },
                    songs = songs
                )
            }
        } catch (e: Exception) {
            Log.e("SoundPod-JioSaavn", "Failed to get album details for $albumId", e)
            null
        }
    }

    private fun normalize(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), "")
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    fun resolveSongUrl(songId: String, title: String?, artistsText: String?): String? {
        if (songId.startsWith("http")) return songId

        val targetTitle = title?.let { normalize(it) } ?: ""
        val targetArtists = artistsText?.let { normalize(it) } ?: ""

        val queries = listOfNotNull(
            if (targetTitle.isNotBlank()) "$targetTitle $targetArtists" else null,
            targetTitle.takeIf { it.isNotBlank() },
            songId
        ).distinct()

        for (query in queries) {
            val results = searchSongs(query)
            if (results.isEmpty()) continue

            var bestMatch: JioSaavnSong? = null
            var bestScore = -1.0

            for (song in results) {
                val songTitle = normalize(song.title)
                val songArtists = normalize(song.artistsText ?: "")

                var score = 0.0
                if (targetTitle.isNotBlank()) {
                    if (songTitle == targetTitle) {
                        score += 10.0
                    } else if (songTitle.contains(targetTitle) || targetTitle.contains(songTitle)) {
                        score += 5.0
                    } else {
                        val targetTokens = targetTitle.split("\\s+".toRegex()).toSet()
                        val songTokens = songTitle.split("\\s+".toRegex()).toSet()
                        val intersection = targetTokens.intersect(songTokens)
                        if (intersection.isNotEmpty()) {
                            score += (intersection.size.toDouble() / targetTokens.size.toDouble()) * 3.0
                        }
                    }
                }

                if (targetArtists.isNotBlank() && songArtists.isNotBlank()) {
                    if (songArtists.contains(targetArtists) || targetArtists.contains(songArtists)) {
                        score += 4.0
                    } else {
                        val targetTokens = targetArtists.split("\\s+".toRegex()).toSet()
                        val songTokens = songArtists.split("\\s+".toRegex()).toSet()
                        val intersection = targetTokens.intersect(songTokens)
                        if (intersection.isNotEmpty()) {
                            score += (intersection.size.toDouble() / targetTokens.size.toDouble()) * 2.0
                        }
                    }
                }

                if (score > bestScore) {
                    bestScore = score
                    bestMatch = song
                }
            }

            if (bestMatch != null && bestMatch.streamUrl != null) {
                if (targetTitle.isBlank() || bestScore >= 2.0 || results.size == 1) {
                    Log.d("SoundPod-JioSaavn", "Matched '$title' -> Best match: '${bestMatch.title}' by '${bestMatch.artistsText}' with score $bestScore")
                    return bestMatch.streamUrl
                }
            }
        }

        return null
    }
}

val JioSaavnSong.asMediaItem: androidx.media3.common.MediaItem
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    get() = androidx.media3.common.MediaItem.Builder()
        .setMediaId(id)
        .setUri(streamUrl ?: id)
        .setCustomCacheKey(id)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setAlbumTitle(albumTitle)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    android.os.Bundle().apply {
                        durationText?.let { putString("durationText", it) }
                    }
                )
                .build()
        )
        .build()

val JioSaavnSong.asSong: com.github.soundpod.models.Song
    get() = com.github.soundpod.models.Song(
        id = id,
        title = title,
        artistsText = artistsText,
        durationText = durationText,
        thumbnailUrl = thumbnailUrl
    )

