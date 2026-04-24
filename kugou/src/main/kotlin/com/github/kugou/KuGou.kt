package com.github.kugou

import com.github.kugou.models.DownloadLyricsResponse
import com.github.kugou.models.SearchLyricsResponse
import com.github.kugou.models.SearchSongResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

object KuGou {
    @OptIn(ExperimentalSerializationApi::class)
    private val client by lazy {
        HttpClient(OkHttp) {
            BrowserUserAgent()

            expectSuccess = true

            install(ContentNegotiation) {
                val feature = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                }

                json(feature)
                json(feature, ContentType.Text.Html)
                json(feature, ContentType.Text.Plain)
            }

            install(ContentEncoding) {
                gzip()
                deflate()
            }

            defaultRequest {
                url("https://krcs.kugou.com")
            }
        }
    }

    suspend fun lyrics(artist: String, title: String, duration: Long): Result<Lyrics?>? {
        return runCatching {
            val keyword = keyword(artist, title)
            val infoByKeyword = searchSong(keyword)

            if (infoByKeyword.isNotEmpty()) {
                var tolerance = 0

                while (tolerance <= 5) {
                    for (info in infoByKeyword) {
                        if (info.duration >= duration - tolerance && info.duration <= duration + tolerance) {
                            searchLyricsByHash(info.hash).firstOrNull()?.let { candidate ->
                                return@runCatching downloadLyrics(candidate.id, candidate.accessKey).normalize()
                            }
                        }
                    }

                    tolerance++
                }
            }

            searchLyricsByKeyword(keyword).firstOrNull()?.let { candidate ->
                return@runCatching downloadLyrics(candidate.id, candidate.accessKey).normalize()
            }

            null
        }.recoverIfCancelled()
    }

    private suspend fun downloadLyrics(id: Long, accessKey: String): Lyrics {
        return Base64.decode(client.get("/download") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter("fmt", "lrc")
            parameter("id", id)
            parameter("accesskey", accessKey)
        }.body<DownloadLyricsResponse>().content).decodeToString().let(::Lyrics)
    }

    private suspend fun searchLyricsByHash(hash: String): List<SearchLyricsResponse.Candidate> {
        return client.get("/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "mobi")
            parameter("hash", hash)
        }.body<SearchLyricsResponse>().candidates
    }

    private suspend fun searchLyricsByKeyword(keyword: String): List<SearchLyricsResponse.Candidate> {
        return client.get("/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "mobi")
            url.encodedParameters.append("keyword", keyword.encodeURLParameter(spaceToPlus = false))
        }.body<SearchLyricsResponse>().candidates
    }

    private suspend fun searchSong(keyword: String): List<SearchSongResponse.Data.Info> {
        return client.get("https://mobileservice.kugou.com/api/v3/search/song") {
            parameter("version", 9108)
            parameter("plat", 0)
            parameter("pagesize", 8)
            parameter("showtype", 0)
            url.encodedParameters.append("keyword", keyword.encodeURLParameter(spaceToPlus = false))
        }.body<SearchSongResponse>().data.info
    }

    private fun keyword(artist: String, title: String): String {
        val (newTitle, featuring) = title.extract(" (feat. ", ')')

        val newArtist = (if (featuring.isEmpty()) artist else "$artist, $featuring")
            .replace(", ", "、")
            .replace(" & ", "、")
            .replace(".", "")

        return "$newArtist - $newTitle"
    }

    private fun String.extract(startDelimiter: String, endDelimiter: Char): Pair<String, String> {
        val startIndex = indexOf(startDelimiter)

        if (startIndex == -1) return this to ""

        val endIndex = indexOf(endDelimiter, startIndex)

        if (endIndex == -1) return this to ""

        return removeRange(
            startIndex,
            endIndex + 1
        ) to substring(startIndex + startDelimiter.length, endIndex)
    }

    @JvmInline
    value class Lyrics(val value: String) : CharSequence {

        override val length: Int
            get() = value.length

        override fun get(index: Int): Char = value[index]

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            value.subSequence(startIndex, endIndex)

        override fun toString(): String = value

        val sentences: List<Pair<Long, String>>
            get() = mutableListOf(0L to "").apply {
                for (line in value.trim().lines()) {
                    try {
                        val position = line.take(10).run {
                            get(8).digitToInt() * 10L +
                                    get(7).digitToInt() * 100 +
                                    get(5).digitToInt() * 1000 +
                                    get(4).digitToInt() * 10000 +
                                    get(2).digitToInt() * 60 * 1000 +
                                    get(1).digitToInt() * 600 * 1000
                        }

                        add(position to line.substring(10))
                    } catch (_: Throwable) {
                    }
                }
            }

        fun normalize(): Lyrics {
            val cleanLines = value.replace("\r\n", "\n").trim().lineSequence().filter { line ->
                // 1. Drop LRC metadata tags like [ti:Title], [ar:Artist], [al:Album], [offset:0]
                // Timestamps start with numbers [00:, metadata starts with letters [ar:
                if (line.matches(Regex("^\\[[a-zA-Z]+:.*"))) return@filter false

                // 2. Get purely the text content after the last timestamp bracket "]"
                val textContent = line.substringAfterLast("]").trim()

                // 3. Check for typical credits in both English and Chinese
                val lowerText = textContent.lowercase()
                val isCredit = lowerText.startsWith("written by") ||
                        lowerText.startsWith("lyrics by") ||
                        lowerText.startsWith("composed by") ||
                        lowerText.startsWith("arranged by") ||
                        lowerText.startsWith("produced by") ||
                        lowerText.startsWith("producer") ||
                        lowerText.startsWith("vocal") ||
                        lowerText.startsWith("作词") ||
                        lowerText.startsWith("作曲") ||
                        lowerText.startsWith("编曲") ||
                        lowerText.startsWith("制作人") ||
                        lowerText.startsWith("混音") ||
                        lowerText.startsWith("演唱") ||
                        lowerText.startsWith("词：") ||
                        lowerText.startsWith("曲：") ||
                        lowerText.startsWith("词:") ||
                        lowerText.startsWith("曲:")

                // Keep the line only if it is NOT a credit
                !isCredit
            }.joinToString("\n")

            return Lyrics(cleanLines.removeHtmlEntities())
        }

        private fun String.removeHtmlEntities(): String = replace("&apos;", "'")
    }
}