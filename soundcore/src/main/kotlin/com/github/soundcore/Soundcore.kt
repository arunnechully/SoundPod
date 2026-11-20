package com.github.soundcore

import com.github.innertube.Innertube
import com.github.innertube.models.NavigationEndpoint
import com.github.innertube.models.Thumbnail
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class SoundcoreSong(
    val id: String,
    val title: String,
    val artistsText: String?,
    val durationText: String?,
    val thumbnailUrl: String?,
    val streamUrl: String?,
    val albumTitle: String?
)

data class SoundcoreAlbum(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val artistsText: String? = null,
    val year: String? = null,
    val songs: List<SoundcoreSong> = emptyList()
)

data class SoundcoreArtist(
    val id: String,
    val title: String,
    val thumbnailUrl: String?
)

data class SoundcorePlaylist(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val songCount: Int? = null
)

data class SoundcoreLaunchData(
    val trendingSongs: List<SoundcoreSong> = emptyList(),
    val topAlbums: List<SoundcoreAlbum> = emptyList(),
    val topPlaylists: List<SoundcorePlaylist> = emptyList()
)

@Suppress("unused")
object Soundcore {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

    var cookieProvider: (() -> String?)? = null

    private fun buildRequest(url: String): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://www.jiosaavn.com/")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en;q=0.9")

        runCatching {
            val cookies = cookieProvider?.invoke()
            if (!cookies.isNullOrBlank()) {
                builder.header("Cookie", cookies)
            }
        }

        return builder.build()
    }

    private fun JSONObject.optNonBlankString(key: String): String? {
        return optString(key).trim().takeIf { it.isNotBlank() && it != "null" }
    }

    fun unescapeHtml(text: String?): String? {
        if (text.isNullOrBlank()) return null
        var s = text
        if (s.contains("&")) {
            s = s.replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#039;", "'")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", " ")
                .replace("&ndash;", "–")
                .replace("&mdash;", "—")
                .replace("&hellip;", "…")
            s = s.replace(Regex("&#(\\d+);")) { match ->
                val code = match.groupValues[1].toIntOrNull()
                code?.toChar()?.toString() ?: match.value
            }.replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
                val code = match.groupValues[1].toIntOrNull(16)
                code?.toChar()?.toString() ?: match.value
            }
        }
        val trimmed = s.trim()
        return trimmed.ifBlank { null }
    }

    private fun extractArtistsText(obj: JSONObject, moreInfo: JSONObject?): String? {
        moreInfo?.optNonBlankString("primary_artists")?.let { return unescapeHtml(it) }
        obj.optNonBlankString("primary_artists")?.let { return unescapeHtml(it) }

        moreInfo?.optNonBlankString("singers")?.let { return unescapeHtml(it) }
        obj.optNonBlankString("singers")?.let { return unescapeHtml(it) }

        moreInfo?.optNonBlankString("artist")?.let { return unescapeHtml(it) }
        obj.optNonBlankString("artist")?.let { return unescapeHtml(it) }
        moreInfo?.optNonBlankString("music")?.let { return unescapeHtml(it) }
        obj.optNonBlankString("music")?.let { return unescapeHtml(it) }
        moreInfo?.optNonBlankString("artist_name")?.let { return unescapeHtml(it) }
        obj.optNonBlankString("artist_name")?.let { return unescapeHtml(it) }
        moreInfo?.optNonBlankString("primary_artist")?.let { return unescapeHtml(it) }
        obj.optNonBlankString("primary_artist")?.let { return unescapeHtml(it) }

        val artistMap = moreInfo?.optJSONObject("artistMap") ?: obj.optJSONObject("artistMap")
        if (artistMap != null) {
            val artistNames = mutableListOf<String>()
            val primaryArr = artistMap.optJSONArray("primary_artists")
                ?: artistMap.optJSONArray("artists")
                ?: artistMap.optJSONArray("singers")
            if (primaryArr != null) {
                for (i in 0 until primaryArr.length()) {
                    val artistObj = primaryArr.optJSONObject(i) ?: continue
                    artistObj.optNonBlankString("name")?.let { artistNames.add(it) }
                }
            }
            if (artistNames.isNotEmpty()) {
                return unescapeHtml(artistNames.joinToString(", "))
            }
        }

        val subtitle = obj.optNonBlankString("subtitle") ?: moreInfo?.optNonBlankString("subtitle")
        if (!subtitle.isNullOrBlank()) {
            val candidate = subtitle.substringBefore(" - ").substringBefore(" • ").trim()
            if (candidate.isNotBlank()) {
                return unescapeHtml(candidate)
            }
            return unescapeHtml(subtitle)
        }

        return null
    }

    private object Base64Decoder {
        private val DECODE_TABLE = IntArray(256) { -1 }.apply {
            for (i in 'A'..'Z') this[i.code] = i - 'A'
            for (i in 'a'..'z') this[i.code] = i - 'a' + 26
            for (i in '0'..'9') this[i.code] = i - '0' + 52
            this['+'.code] = 62
            this['/'.code] = 63
        }

        fun decode(input: String): ByteArray {
            val cleanInput = input.replace(Regex("[^A-Za-z0-9+/=]"), "")
            val len = cleanInput.length
            if (len == 0) return ByteArray(0)
            var padding = 0
            if (cleanInput.endsWith("==")) padding = 2
            else if (cleanInput.endsWith("=")) padding = 1

            val outLen = (len * 3) / 4 - padding
            val out = ByteArray(outLen)
            var outIdx = 0

            var i = 0
            while (i < len) {
                val b0 = DECODE_TABLE[cleanInput[i].code]
                val b1 = DECODE_TABLE[cleanInput[i + 1].code]
                val b2 = if (i + 2 < len && cleanInput[i + 2] != '=') DECODE_TABLE[cleanInput[i + 2].code] else 0
                val b3 = if (i + 3 < len && cleanInput[i + 3] != '=') DECODE_TABLE[cleanInput[i + 3].code] else 0

                out[outIdx++] = ((b0 shl 2) or (b1 shr 4)).toByte()
                if (outIdx < outLen) {
                    out[outIdx++] = (((b1 and 0xF) shl 4) or (b2 shr 2)).toByte()
                }
                if (outIdx < outLen) {
                    out[outIdx++] = (((b2 and 0x3) shl 6) or b3).toByte()
                }
                i += 4
            }
            return out
        }
    }

    fun decryptUrl(encryptedUrl: String): String {
        if (encryptedUrl.isBlank()) return ""
        return try {
            var rawEncrypted = encryptedUrl.trim().removeSurrounding("\"")
            if (rawEncrypted.contains("%")) {
                runCatching { rawEncrypted = URLDecoder.decode(rawEncrypted, "UTF-8") }
            }

            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            val key = SecretKeySpec("38346591".toByteArray(Charsets.UTF_8), "DES")
            cipher.init(Cipher.DECRYPT_MODE, key)

            val decodedBytes = Base64Decoder.decode(rawEncrypted)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            var url = String(decryptedBytes, Charsets.UTF_8).trim()

            if (url.startsWith("http://")) {
                url = url.replace("http://", "https://")
            }

            url = url
                .replace("_96.mp4", "_320.mp4")
                .replace("_160.mp4", "_320.mp4")
                .replace("_96.m4a", "_320.m4a")
                .replace("_160.m4a", "_320.m4a")
                .replace("_96_p.mp4", "_320_p.mp4")
                .replace("_160_p.mp4", "_320_p.mp4")

            url
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Failed to decrypt URL: $encryptedUrl - ${e.message}")
            encryptedUrl.replace("http://", "https://")
        }
    }

    private fun parseSongObject(obj: JSONObject): SoundcoreSong? {
        val id = obj.optNonBlankString("id") ?: obj.optNonBlankString("perma_url")
        if (id.isNullOrBlank()) return null

        val rawTitle = obj.optNonBlankString("title") ?: obj.optNonBlankString("song")
        val title = unescapeHtml(rawTitle) ?: return null

        val moreInfo = obj.optJSONObject("more_info")
        val artistsText = extractArtistsText(obj, moreInfo)

        val duration = moreInfo?.optNonBlankString("duration") ?: obj.optNonBlankString("duration")
        val durationText = duration?.toLongOrNull()?.let {
            val min = it / 60
            val sec = it % 60
            String.format(Locale.ROOT, "%d:%02d", min, sec)
        }

        val rawImage = (obj.optNonBlankString("image")
            ?: moreInfo?.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
        val thumbnailUrl = if (rawImage.isNotBlank()) rawImage.replace("http://", "https://") else null

        val encUrl = (moreInfo?.optNonBlankString("encrypted_media_url") ?: obj.optNonBlankString("encrypted_media_url"))
        var streamUrl: String? = encUrl?.let { decryptUrl(it) }
        if (streamUrl.isNullOrBlank()) {
            val mediaUrl = (moreInfo?.optNonBlankString("media_url") ?: obj.optNonBlankString("media_url"))
            streamUrl = mediaUrl?.replace("http://", "https://")
        }

        val albumTitle = unescapeHtml(moreInfo?.optNonBlankString("album") ?: obj.optNonBlankString("album"))

        return SoundcoreSong(
            id = id,
            title = title,
            artistsText = artistsText,
            durationText = durationText,
            thumbnailUrl = thumbnailUrl,
            streamUrl = streamUrl,
            albumTitle = albumTitle
        )
    }

    fun searchSongs(query: String): List<SoundcoreSong> {
        if (query.isBlank()) return emptyList()
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=search.getMoreResults&q=$encodedQuery&cat=songs&n=20&p=1&_format=json&_marker=0&api_version=4"
            val request = buildRequest(url)
            val list = mutableListOf<SoundcoreSong>()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = runCatching { JSONObject(body) }.getOrNull()
                val resultsArray = json?.optJSONArray("results")
                    ?: json?.optJSONArray("songs")
                    ?: runCatching { JSONArray(body) }.getOrNull()
                    ?: JSONArray()

                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.optJSONObject(i) ?: continue
                    parseSongObject(obj)?.let { list.add(it) }
                }
            }

            if (list.isEmpty()) {
                val fallbackUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&q=$encodedQuery&_format=json&_marker=0&api_version=4"
                client.newCall(buildRequest(fallbackUrl)).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val results = json?.optJSONArray("results") ?: JSONArray()
                    for (i in 0 until results.length()) {
                        val obj = results.optJSONObject(i) ?: continue
                        parseSongObject(obj)?.let { list.add(it) }
                    }
                }
            }

            list
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Search songs failed for $query - ${e.message}")
            emptyList()
        }
    }

    fun searchAlbums(query: String): List<SoundcoreAlbum> {
        if (query.isBlank()) return emptyList()
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val list = mutableListOf<SoundcoreAlbum>()

            val url1 = "https://www.jiosaavn.com/api.php?__call=search.getAlbumResults&q=$encodedQuery&n=20&p=1&_format=json&_marker=0&api_version=4"
            client.newCall(buildRequest(url1)).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = runCatching { JSONObject(body) }.getOrNull()
                val resultsArray = json?.optJSONArray("results")
                    ?: json?.optJSONArray("albums")
                    ?: json?.optJSONObject("albums")?.optJSONArray("data")
                    ?: runCatching { JSONArray(body) }.getOrNull()
                    ?: JSONArray()

                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.optJSONObject(i) ?: continue
                    val id = obj.optNonBlankString("id") ?: obj.optNonBlankString("albumid") ?: continue
                    val title = unescapeHtml(obj.optNonBlankString("title") ?: obj.optNonBlankString("name") ?: obj.optNonBlankString("album")) ?: continue
                    val rawImage = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                    val thumbnailUrl = if (rawImage.isNotBlank()) rawImage.replace("http://", "https://") else null
                    val moreInfo = obj.optJSONObject("more_info")
                    val artistsText = extractArtistsText(obj, moreInfo)
                    val year = moreInfo?.optNonBlankString("year") ?: obj.optNonBlankString("year")

                    list.add(
                        SoundcoreAlbum(
                            id = id,
                            title = title,
                            thumbnailUrl = thumbnailUrl,
                            artistsText = artistsText,
                            year = year
                        )
                    )
                }
            }

            if (list.isEmpty()) {
                val url2 = "https://www.jiosaavn.com/api.php?__call=search.getResults&q=$encodedQuery&_format=json&_marker=0&api_version=4"
                client.newCall(buildRequest(url2)).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val albumsArray = json?.optJSONObject("albums")?.optJSONArray("data")
                        ?: json?.optJSONArray("albums")
                        ?: JSONArray()

                    for (i in 0 until albumsArray.length()) {
                        val obj = albumsArray.optJSONObject(i) ?: continue
                        val id = obj.optNonBlankString("id") ?: obj.optNonBlankString("albumid") ?: continue
                        val title = unescapeHtml(obj.optNonBlankString("title") ?: obj.optNonBlankString("name") ?: obj.optNonBlankString("album")) ?: continue
                        val rawImage = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                        val thumbnailUrl = if (rawImage.isNotBlank()) rawImage.replace("http://", "https://") else null
                        val moreInfo = obj.optJSONObject("more_info")
                        val artistsText = extractArtistsText(obj, moreInfo)
                        val year = moreInfo?.optNonBlankString("year") ?: obj.optNonBlankString("year")

                        list.add(
                            SoundcoreAlbum(
                                id = id,
                                title = title,
                                thumbnailUrl = thumbnailUrl,
                                artistsText = artistsText,
                                year = year
                            )
                        )
                    }
                }
            }

            if (list.isEmpty()) {
                val url3 = "https://www.jiosaavn.com/api.php?__call=search.getMoreResults&q=$encodedQuery&cat=albums&n=20&p=1&_format=json&_marker=0&api_version=4"
                client.newCall(buildRequest(url3)).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val resultsArray = json?.optJSONArray("results") ?: json?.optJSONArray("albums") ?: JSONArray()
                    for (i in 0 until resultsArray.length()) {
                        val obj = resultsArray.optJSONObject(i) ?: continue
                        val id = obj.optNonBlankString("id") ?: obj.optNonBlankString("albumid") ?: continue
                        val title = unescapeHtml(obj.optNonBlankString("title") ?: obj.optNonBlankString("name") ?: obj.optNonBlankString("album")) ?: continue
                        val rawImage = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                        val thumbnailUrl = if (rawImage.isNotBlank()) rawImage.replace("http://", "https://") else null
                        val moreInfo = obj.optJSONObject("more_info")
                        val artistsText = extractArtistsText(obj, moreInfo)
                        val year = moreInfo?.optNonBlankString("year") ?: obj.optNonBlankString("year")

                        list.add(
                            SoundcoreAlbum(
                                id = id,
                                title = title,
                                thumbnailUrl = thumbnailUrl,
                                artistsText = artistsText,
                                year = year
                            )
                        )
                    }
                }
            }

            list.distinctBy { it.id }
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Search albums failed for $query - ${e.message}")
            emptyList()
        }
    }

    fun searchArtists(query: String): List<SoundcoreArtist> {
        if (query.isBlank()) return emptyList()
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val list = mutableListOf<SoundcoreArtist>()

            val url1 = "https://www.jiosaavn.com/api.php?__call=search.getArtistResults&q=$encodedQuery&n=20&p=1&_format=json&_marker=0&api_version=4"
            client.newCall(buildRequest(url1)).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = runCatching { JSONObject(body) }.getOrNull()
                val resultsArray = json?.optJSONArray("results")
                    ?: json?.optJSONArray("artists")
                    ?: json?.optJSONObject("artists")?.optJSONArray("data")
                    ?: runCatching { JSONArray(body) }.getOrNull()
                    ?: JSONArray()

                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.optJSONObject(i) ?: continue
                    val id = obj.optNonBlankString("id") ?: obj.optNonBlankString("artistid") ?: continue
                    val title = unescapeHtml(obj.optNonBlankString("name") ?: obj.optNonBlankString("title") ?: obj.optNonBlankString("artist")) ?: continue
                    val rawImage = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                    val thumbnailUrl = if (rawImage.isNotBlank()) rawImage.replace("http://", "https://") else null

                    list.add(
                        SoundcoreArtist(
                            id = id,
                            title = title,
                            thumbnailUrl = thumbnailUrl
                        )
                    )
                }
            }

            if (list.isEmpty()) {
                val url2 = "https://www.jiosaavn.com/api.php?__call=search.getResults&q=$encodedQuery&_format=json&_marker=0&api_version=4"
                client.newCall(buildRequest(url2)).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val artistsArray = json?.optJSONObject("artists")?.optJSONArray("data")
                        ?: json?.optJSONArray("artists")
                        ?: JSONArray()

                    for (i in 0 until artistsArray.length()) {
                        val obj = artistsArray.optJSONObject(i) ?: continue
                        val id = obj.optNonBlankString("id") ?: obj.optNonBlankString("artistid") ?: continue
                        val title = unescapeHtml(obj.optNonBlankString("name") ?: obj.optNonBlankString("title") ?: obj.optNonBlankString("artist")) ?: continue
                        val rawImage = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                        val thumbnailUrl = if (rawImage.isNotBlank()) rawImage.replace("http://", "https://") else null

                        list.add(
                            SoundcoreArtist(
                                id = id,
                                title = title,
                                thumbnailUrl = thumbnailUrl
                            )
                        )
                    }
                }
            }

            if (list.isEmpty()) {
                val url3 = "https://www.jiosaavn.com/api.php?__call=search.getMoreResults&q=$encodedQuery&cat=artists&n=20&p=1&_format=json&_marker=0&api_version=4"
                client.newCall(buildRequest(url3)).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val resultsArray = json?.optJSONArray("results") ?: json?.optJSONArray("artists") ?: JSONArray()
                    for (i in 0 until resultsArray.length()) {
                        val obj = resultsArray.optJSONObject(i) ?: continue
                        val id = obj.optNonBlankString("id") ?: obj.optNonBlankString("artistid") ?: continue
                        val title = unescapeHtml(obj.optNonBlankString("name") ?: obj.optNonBlankString("title") ?: obj.optNonBlankString("artist")) ?: continue
                        val rawImage = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                        val thumbnailUrl = if (rawImage.isNotBlank()) rawImage.replace("http://", "https://") else null

                        list.add(
                            SoundcoreArtist(
                                id = id,
                                title = title,
                                thumbnailUrl = thumbnailUrl
                            )
                        )
                    }
                }
            }

            list.distinctBy { it.id }
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Search artists failed for $query - ${e.message}")
            emptyList()
        }
    }

    fun searchPlaylists(query: String): List<SoundcorePlaylist> {
        if (query.isBlank()) return emptyList()
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=search.getMoreResults&q=$encodedQuery&cat=playlists&n=20&p=1&_format=json&_marker=0&api_version=4"
            val list = mutableListOf<SoundcorePlaylist>()

            client.newCall(buildRequest(url)).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = runCatching { JSONObject(body) }.getOrNull()
                val resultsArray = json?.optJSONArray("results") ?: json?.optJSONArray("playlists") ?: JSONArray()

                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.optJSONObject(i) ?: continue
                    val id = obj.optNonBlankString("id") ?: obj.optNonBlankString("listid") ?: continue
                    val title = unescapeHtml(obj.optNonBlankString("title") ?: obj.optNonBlankString("listname") ?: obj.optNonBlankString("name")) ?: continue
                    val rawImage = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                    val thumbnailUrl = if (rawImage.isNotBlank()) rawImage.replace("http://", "https://") else null
                    val songCount = obj.optJSONObject("more_info")?.optNonBlankString("song_count")?.toIntOrNull() ?: obj.optNonBlankString("song_count")?.toIntOrNull()

                    list.add(
                        SoundcorePlaylist(
                            id = id,
                            title = title,
                            thumbnailUrl = thumbnailUrl,
                            songCount = songCount
                        )
                    )
                }
            }
            list
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Search playlists failed for $query - ${e.message}")
            emptyList()
        }
    }

    fun getSongDetails(songId: String): SoundcoreSong? {
        if (songId.isBlank()) return null
        return try {
            val url = "https://www.jiosaavn.com/api.php?__call=song.getDetails&cc=in&_marker=0&_format=json&api_version=4&pids=$songId"
            client.newCall(buildRequest(url)).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)

                val songObj = json.optJSONObject(songId)
                    ?: json.optJSONArray("songs")?.optJSONObject(0)
                    ?: json.keys().asSequence().firstOrNull()?.let { json.optJSONObject(it) }

                songObj?.let { parseSongObject(it) }
            }
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Failed to get song details for $songId - ${e.message}")
            null
        }
    }

    fun getRecommendations(songId: String): List<SoundcoreSong> {
        if (songId.isBlank()) return emptyList()
        return try {
            val url = "https://www.jiosaavn.com/api.php?__call=reco.getreco&pid=$songId&_format=json&_marker=0&api_version=4"
            val list = mutableListOf<SoundcoreSong>()

            client.newCall(buildRequest(url)).execute().use { response ->
                val body = response.body?.string() ?: ""
                val jsonArray = runCatching { JSONArray(body) }.getOrNull()
                    ?: runCatching { JSONObject(body).optJSONArray("songs") }.getOrNull()
                    ?: runCatching { JSONObject(body).optJSONArray("results") }.getOrNull()
                    ?: JSONArray()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    parseSongObject(obj)?.let { list.add(it) }
                }
            }
            list
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Failed to get recommendations for $songId - ${e.message}")
            emptyList()
        }
    }

    fun getLaunchData(): SoundcoreLaunchData {
        return try {
            val url = "https://www.jiosaavn.com/api.php?__call=launch.getLaunchData&_format=json&_marker=0&api_version=4"
            client.newCall(buildRequest(url)).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)

                val trendingSongs = mutableListOf<SoundcoreSong>()
                val topAlbums = mutableListOf<SoundcoreAlbum>()
                val topPlaylists = mutableListOf<SoundcorePlaylist>()

                val trendingArray = json.optJSONArray("new_trending") ?: json.optJSONArray("top_shows") ?: JSONArray()
                for (i in 0 until trendingArray.length()) {
                    val obj = trendingArray.optJSONObject(i) ?: continue
                    val type = obj.optNonBlankString("type")
                    if (type == "song" || obj.has("song")) {
                        parseSongObject(obj)?.let { trendingSongs.add(it) }
                    } else if (type == "album") {
                        val id = obj.optNonBlankString("id") ?: continue
                        val title = unescapeHtml(obj.optNonBlankString("title") ?: obj.optNonBlankString("name"))
                        val rawImg = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                        val img = if (rawImg.isNotBlank()) rawImg.replace("http://", "https://") else null
                        val artistsText = extractArtistsText(obj, obj.optJSONObject("more_info"))
                        title?.let {
                            topAlbums.add(SoundcoreAlbum(id = id, title = it, thumbnailUrl = img, artistsText = artistsText))
                        }
                    }
                }

                val playlistsArray = json.optJSONArray("top_playlists") ?: json.optJSONArray("charts") ?: JSONArray()
                for (i in 0 until playlistsArray.length()) {
                    val obj = playlistsArray.optJSONObject(i) ?: continue
                    val id = obj.optNonBlankString("id") ?: obj.optNonBlankString("listid") ?: continue
                    val title = unescapeHtml(obj.optNonBlankString("title") ?: obj.optNonBlankString("listname"))
                    val rawImg = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                    val img = if (rawImg.isNotBlank()) rawImg.replace("http://", "https://") else null
                    title?.let {
                        topPlaylists.add(SoundcorePlaylist(id = id, title = it, thumbnailUrl = img))
                    }
                }

                val albumsArray = json.optJSONArray("new_albums") ?: JSONArray()
                for (i in 0 until albumsArray.length()) {
                    val obj = albumsArray.optJSONObject(i) ?: continue
                    val id = obj.optNonBlankString("id") ?: continue
                    val title = unescapeHtml(obj.optNonBlankString("title") ?: obj.optNonBlankString("name"))
                    val rawImg = (obj.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                    val img = if (rawImg.isNotBlank()) rawImg.replace("http://", "https://") else null
                    val artistsText = extractArtistsText(obj, obj.optJSONObject("more_info"))
                    title?.takeIf { topAlbums.none { a -> a.id == id } }?.let {
                        topAlbums.add(SoundcoreAlbum(id = id, title = it, thumbnailUrl = img, artistsText = artistsText))
                    }
                }

                SoundcoreLaunchData(
                    trendingSongs = trendingSongs,
                    topAlbums = topAlbums,
                    topPlaylists = topPlaylists
                )
            }
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Failed to fetch launch data - ${e.message}")
            SoundcoreLaunchData()
        }
    }

    fun getAlbumDetails(albumId: String): SoundcoreAlbum? {
        if (albumId.isBlank()) return null
        return try {
            val url = "https://www.jiosaavn.com/api.php?__call=content.getAlbumDetails&albumid=$albumId&_format=json&_marker=0&api_version=4&ctx=web6dot0"
            client.newCall(buildRequest(url)).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val rawTitle = json.optNonBlankString("title") ?: json.optNonBlankString("name")
                val title = unescapeHtml(rawTitle) ?: "Album"
                val rawImage = (json.optNonBlankString("image") ?: "").replace("150x150", "500x500").replace("50x50", "500x500")
                val thumbnailUrl = if (rawImage.isNotBlank()) rawImage.replace("http://", "https://") else null
                val artistsText = extractArtistsText(json, json.optJSONObject("more_info"))
                val year = json.optNonBlankString("year") ?: json.optJSONObject("more_info")?.optNonBlankString("year")

                val songsArray = json.optJSONArray("songs") ?: JSONArray()
                val songs = mutableListOf<SoundcoreSong>()
                for (i in 0 until songsArray.length()) {
                    val obj = songsArray.optJSONObject(i) ?: continue
                    parseSongObject(obj)?.let { songs.add(it) }
                }

                SoundcoreAlbum(
                    id = albumId,
                    title = title,
                    thumbnailUrl = thumbnailUrl,
                    artistsText = artistsText,
                    year = year,
                    songs = songs
                )
            }
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Failed to get album details for $albumId - ${e.message}")
            null
        }
    }

    fun getSearchSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&_format=json&_marker=0&api_version=4&query=$encodedQuery"
            client.newCall(buildRequest(url)).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val suggestions = LinkedHashSet<String>()

                json.optJSONObject("topquery")?.optJSONArray("data")?.let { topQueryData ->
                    for (i in 0 until topQueryData.length()) {
                        val item = topQueryData.optJSONObject(i) ?: continue
                        val title = unescapeHtml(item.optNonBlankString("title") ?: item.optNonBlankString("query"))
                        if (!title.isNullOrBlank()) suggestions.add(title)
                    }
                }

                json.optJSONObject("songs")?.optJSONArray("data")?.let { songsData ->
                    for (i in 0 until songsData.length()) {
                        val item = songsData.optJSONObject(i) ?: continue
                        val title = unescapeHtml(item.optNonBlankString("title"))
                        if (!title.isNullOrBlank()) suggestions.add(title)
                    }
                }

                json.optJSONObject("albums")?.optJSONArray("data")?.let { albumsData ->
                    for (i in 0 until albumsData.length()) {
                        val item = albumsData.optJSONObject(i) ?: continue
                        val title = unescapeHtml(item.optNonBlankString("title"))
                        if (!title.isNullOrBlank()) suggestions.add(title)
                    }
                }

                suggestions.toList().take(10)
            }
        } catch (e: Exception) {
            System.err.println("SoundPod-Soundcore: Failed to get suggestions for $query - ${e.message}")
            emptyList()
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
        if (songId.startsWith("http://") || songId.startsWith("https://")) return songId

        val directSong = getSongDetails(songId)
        val directUrl = directSong?.streamUrl
        if (directUrl != null) {
            return directUrl
        }

        val targetTitle = title?.let { normalize(it) } ?: ""
        val targetArtists = artistsText?.let { normalize(it) } ?: ""

        val queries = listOfNotNull(
            if (targetTitle.isNotBlank()) "$targetTitle $targetArtists" else null,
            targetTitle.takeIf { it.isNotBlank() },
            songId.takeIf { !it.contains("http") }
        ).distinct()

        for (query in queries) {
            val results = searchSongs(query)
            if (results.isEmpty()) continue

            var bestMatch: SoundcoreSong? = null
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

            val streamUrl = bestMatch?.streamUrl
            if (streamUrl != null) {
                if (targetTitle.isBlank() || bestScore >= 2.0 || results.size == 1) {
                    return streamUrl
                }
            }
        }

        return null
    }
}

val SoundcoreSong.asSoundcoreSongItem: Innertube.SongItem
    get() = Innertube.SongItem(
        info = Innertube.Info(
            name = title,
            endpoint = NavigationEndpoint.Endpoint.Watch(videoId = id)
        ),
        authors = artistsText?.takeIf { it.isNotBlank() }?.let { artists ->
            listOf(Innertube.Info(name = artists, endpoint = NavigationEndpoint.Endpoint.Browse(browseId = "soundcore_artist:$id")))
        },
        album = albumTitle?.let {
            Innertube.Info(name = it, endpoint = null)
        },
        durationText = durationText,
        thumbnail = thumbnailUrl?.let { Thumbnail(url = it, width = 500, height = 500) }
    )

val SoundcoreAlbum.asSoundcoreAlbumItem: Innertube.AlbumItem
    get() = Innertube.AlbumItem(
        info = Innertube.Info(
            name = title,
            endpoint = NavigationEndpoint.Endpoint.Browse(browseId = "soundcore_album:$id")
        ),
        authors = artistsText?.takeIf { it.isNotBlank() }?.let {
            listOf(Innertube.Info(name = it, endpoint = null))
        },
        year = year,
        thumbnail = thumbnailUrl?.let { Thumbnail(url = it, width = 500, height = 500) }
    )

val SoundcoreArtist.asSoundcoreArtistItem: Innertube.ArtistItem
    get() = Innertube.ArtistItem(
        info = Innertube.Info(
            name = title,
            endpoint = NavigationEndpoint.Endpoint.Browse(browseId = "soundcore_artist:$id")
        ),
        subscribersCountText = null,
        thumbnail = thumbnailUrl?.let { Thumbnail(url = it, width = 500, height = 500) }
    )

val SoundcorePlaylist.asSoundcorePlaylistItem: Innertube.PlaylistItem
    get() = Innertube.PlaylistItem(
        info = Innertube.Info(
            name = title,
            endpoint = NavigationEndpoint.Endpoint.Browse(browseId = "soundcore_playlist:$id")
        ),
        channel = null,
        songCount = songCount,
        thumbnail = thumbnailUrl?.let { Thumbnail(url = it, width = 500, height = 500) }
    )
