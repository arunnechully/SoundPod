package com.github.soundcore

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SoundcoreMatcher {
    const val ACCEPT_THRESHOLD = 5.0

    data class SongSignals(
        val title: String,
        val artists: String,
        val album: String?,
        val durationSeconds: Long?,
    )

    data class ArtistSignals(
        val name: String,
    )

    data class AlbumSignals(
        val title: String,
        val artists: String?,
        val year: String?,
    )

    private val FEAT_PATTERN = Regex(
        """(?i)\s*(feat\.?|ft\.?|featuring|with|prod\.?|produced by)\s+[^()]+"""
    )
    private val PARENS_PATTERN = Regex("""\([^)]*\)|\[[^\]]*]""")
    private val NON_ALPHANUM = Regex("""[^a-z0-9\s]""")
    private val MULTI_SPACE = Regex("""\s+""")

    fun normalize(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace(PARENS_PATTERN, " ")
            .replace(NON_ALPHANUM, " ")
            .replace(MULTI_SPACE, " ")
            .trim()

    private fun normalizeSoft(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace(NON_ALPHANUM, " ")
            .replace(MULTI_SPACE, " ")
            .trim()

    fun removeFeat(text: String): String =
        text.replace(FEAT_PATTERN, "").replace(PARENS_PATTERN, "").trim()

    private fun tokenSet(text: String): Set<String> =
        normalize(text).split(MULTI_SPACE).filter { it.length > 1 }.toSet()

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        val union = (a + b).size
        if (union == 0) return 0.0
        return a.intersect(b).size.toDouble() / union
    }

    fun parseDurationSeconds(durationText: String?): Long? {
        if (durationText.isNullOrBlank()) return null
        val parts = durationText.trim().split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    fun score(query: SongSignals, candidate: SoundcoreSong): Double {
        var total = 0.0

        val qTitle = normalize(removeFeat(query.title))
        val cTitle = normalize(removeFeat(candidate.title))

        val qTitleSoft = normalizeSoft(query.title)

        total += when {
            qTitle == cTitle -> 10.0
            qTitle.isBlank() || cTitle.isBlank() -> 0.0
            qTitle.contains(cTitle) || cTitle.contains(qTitle) -> 7.0
            qTitleSoft.contains(normalizeSoft(candidate.title)) ||
                    normalizeSoft(candidate.title).contains(qTitleSoft) -> 5.5
            else -> {
                val jac = jaccard(tokenSet(query.title), tokenSet(candidate.title))
                jac * 5.0
            }
        }

        if (total == 0.0) return 0.0

        val qArtists = normalize(query.artists)
        val cArtists = normalize(candidate.artistsText ?: "")

        if (qArtists.isNotBlank() && cArtists.isNotBlank()) {
            total += when {
                qArtists == cArtists -> 5.0
                qArtists.contains(cArtists) || cArtists.contains(qArtists) -> 3.0
                else -> {
                    val jac = jaccard(tokenSet(query.artists), tokenSet(candidate.artistsText ?: ""))
                    jac * 2.0
                }
            }
        }

        val qAlbum = query.album?.let { normalize(it) }
        val cAlbum = candidate.albumTitle?.let { normalize(it) }
        if (!qAlbum.isNullOrBlank() && !cAlbum.isNullOrBlank()) {
            val jac = jaccard(tokenSet(qAlbum), tokenSet(cAlbum))
            total += jac * 2.0
        }

        val qDur = query.durationSeconds
        val cDur = parseDurationSeconds(candidate.durationText)
        if (qDur != null && cDur != null) {
            val diff = abs(qDur - cDur)
            total += when {
                diff == 0L -> 3.0
                diff <= 2 -> 2.5
                diff <= 5 -> 1.5
                diff <= 10 -> 0.5
                else -> 0.0
            }
        }

        return total
    }

    fun findBestMatch(
        query: SongSignals,
        candidates: List<SoundcoreSong>,
    ): SoundcoreSong? {
        if (candidates.isEmpty()) return null

        var bestSong: SoundcoreSong? = null
        var bestScore = Double.MIN_VALUE

        for (candidate in candidates) {
            val s = score(query, candidate)
            if (s > bestScore) {
                bestScore = s
                bestSong = candidate
            }
        }

        return if (bestScore >= ACCEPT_THRESHOLD) bestSong else null
    }

    private const val FALLBACK_THRESHOLD = 3.0

    fun buildSearchQueries(signals: SongSignals): List<String> {
        val cleanTitle = removeFeat(signals.title).trim()
        val normalTitle = normalize(cleanTitle)
        val artists = signals.artists.trim()
        val firstArtist = artists.split(",", "&", "feat", "ft").first().trim()

        return buildList {
            if (normalTitle.isNotBlank() && firstArtist.isNotBlank()) {
                add("$cleanTitle $firstArtist")
            }
            if (normalTitle.isNotBlank() && artists.isNotBlank() && artists != firstArtist) {
                add("$cleanTitle $artists")
            }
            if (normalTitle.isNotBlank()) {
                add(cleanTitle)
            }
            if (signals.title != cleanTitle && firstArtist.isNotBlank()) {
                add("${signals.title} $firstArtist")
            }
            val album = signals.album?.trim()
            if (!album.isNullOrBlank() && normalTitle.isNotBlank()) {
                add("$album $cleanTitle")
            }
        }.distinct().filter { it.isNotBlank() }
    }

    fun resolveMatch(
        signals: SongSignals,
        searcher: (query: String) -> List<SoundcoreSong>,
        maxQueriesRun: Int = 3,
    ): SoundcoreSong? {
        val queries = buildSearchQueries(signals)

        var globalBest: SoundcoreSong? = null
        var globalBestScore = Double.MIN_VALUE

        for ((index, query) in queries.withIndex()) {
            if (index >= maxQueriesRun) break

            val candidates = searcher(query)
            if (candidates.isEmpty()) continue

            var localBest: SoundcoreSong? = null
            var localBestScore = Double.MIN_VALUE

            for (candidate in candidates) {
                val s = score(signals, candidate)
                if (s > localBestScore) {
                    localBestScore = s
                    localBest = candidate
                }
                if (s > globalBestScore) {
                    globalBestScore = s
                    globalBest = candidate
                }
            }

            if (localBestScore >= ACCEPT_THRESHOLD && localBest != null) {
                return localBest
            }
        }

        return if (globalBestScore >= FALLBACK_THRESHOLD) globalBest else null
    }

    private const val ARTIST_ACCEPT_THRESHOLD = 6.0
    private const val ARTIST_FALLBACK_THRESHOLD = 4.0

    fun scoreArtist(query: ArtistSignals, candidate: SoundcoreArtist): Double {
        val qName = normalize(query.name)
        val cName = normalize(candidate.title)
        if (qName.isBlank() || cName.isBlank()) return 0.0

        return when {
            qName == cName -> 10.0
            qName.contains(cName) || cName.contains(qName) -> 7.0
            else -> {
                val jac = jaccard(tokenSet(query.name), tokenSet(candidate.title))
                if (jac > 0.0) {
                    val lev = levenshteinSimilarity(qName, cName)
                    jac * 4.0 + lev * 3.0
                } else 0.0
            }
        }
    }

    fun buildArtistSearchQueries(signals: ArtistSignals): List<String> {
        val name = signals.name.trim()
        val normalized = normalize(name)
        return buildList {
            if (name.isNotBlank()) add(name)
            if (normalized != name.lowercase(Locale.ROOT) && normalized.isNotBlank()) add(normalized)
        }.distinct().filter { it.isNotBlank() }
    }

    fun resolveArtistMatch(
        signals: ArtistSignals,
        searcher: (query: String) -> List<SoundcoreArtist>,
        maxQueriesRun: Int = 2,
    ): SoundcoreArtist? {
        val queries = buildArtistSearchQueries(signals)
        var globalBest: SoundcoreArtist? = null
        var globalBestScore = Double.MIN_VALUE

        for ((index, query) in queries.withIndex()) {
            if (index >= maxQueriesRun) break
            val candidates = searcher(query)
            if (candidates.isEmpty()) continue

            for (candidate in candidates) {
                val s = scoreArtist(signals, candidate)
                if (s > globalBestScore) {
                    globalBestScore = s
                    globalBest = candidate
                }
                if (s >= ARTIST_ACCEPT_THRESHOLD) return candidate
            }
        }

        return if (globalBestScore >= ARTIST_FALLBACK_THRESHOLD) globalBest else null
    }

    private const val ALBUM_ACCEPT_THRESHOLD = 6.0
    private const val ALBUM_FALLBACK_THRESHOLD = 4.0

    fun scoreAlbum(query: AlbumSignals, candidate: SoundcoreAlbum): Double {
        var total = 0.0
        val qTitle = normalize(query.title)
        val cTitle = normalize(candidate.title)
        if (qTitle.isBlank() || cTitle.isBlank()) return 0.0

        total += when {
            qTitle == cTitle -> 10.0
            qTitle.contains(cTitle) || cTitle.contains(qTitle) -> 7.0
            else -> {
                val jac = jaccard(tokenSet(query.title), tokenSet(candidate.title))
                jac * 5.0
            }
        }

        if (total == 0.0) return 0.0

        val qArtists = query.artists?.let { normalize(it) }.orEmpty()
        val cArtists = normalize(candidate.artistsText ?: "")
        if (qArtists.isNotBlank() && cArtists.isNotBlank()) {
            total += when {
                qArtists == cArtists -> 5.0
                qArtists.contains(cArtists) || cArtists.contains(qArtists) -> 3.0
                else -> {
                    val jac = jaccard(tokenSet(qArtists), tokenSet(cArtists))
                    jac * 2.0
                }
            }
        }

        val qYear = query.year?.trim()
        val cYear = candidate.year?.trim()
        if (!qYear.isNullOrBlank() && !cYear.isNullOrBlank() && qYear == cYear) {
            total += 2.0
        }

        return total
    }

    fun buildAlbumSearchQueries(signals: AlbumSignals): List<String> {
        val title = signals.title.trim()
        val artists = signals.artists?.trim().orEmpty()
        val firstArtist = artists.split(",", "&").first().trim()
        return buildList {
            if (title.isNotBlank() && firstArtist.isNotBlank()) add("$title $firstArtist")
            if (title.isNotBlank() && artists.isNotBlank() && artists != firstArtist) add("$title $artists")
            if (title.isNotBlank()) add(title)
        }.distinct().filter { it.isNotBlank() }
    }

    fun resolveAlbumMatch(
        signals: AlbumSignals,
        searcher: (query: String) -> List<SoundcoreAlbum>,
        maxQueriesRun: Int = 3,
    ): SoundcoreAlbum? {
        val queries = buildAlbumSearchQueries(signals)
        var globalBest: SoundcoreAlbum? = null
        var globalBestScore = Double.MIN_VALUE

        for ((index, query) in queries.withIndex()) {
            if (index >= maxQueriesRun) break
            val candidates = searcher(query)
            if (candidates.isEmpty()) continue

            for (candidate in candidates) {
                val s = scoreAlbum(signals, candidate)
                if (s > globalBestScore) {
                    globalBestScore = s
                    globalBest = candidate
                }
                if (s >= ALBUM_ACCEPT_THRESHOLD) return candidate
            }
        }

        return if (globalBestScore >= ALBUM_FALLBACK_THRESHOLD) globalBest else null
    }

    fun levenshteinSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val m = a.length; val n = b.length
        if (m == 0 || n == 0) return 0.0
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else min(dp[i - 1][j - 1], min(dp[i - 1][j], dp[i][j - 1])) + 1
        }
        return 1.0 - dp[m][n].toDouble() / max(m, n)
    }
}
