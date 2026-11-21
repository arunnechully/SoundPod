package com.github.soundcore

object SoundcoreRecommender {

    data class RecommendationScore(
        val song: SoundcoreSong,
        val score: Double,
    )

    fun getRecommendedSongs(
        seedSongIds: List<String>,
        seedArtistNames: List<String> = emptyList(),
        excludeSongIds: Set<String> = emptySet(),
        limit: Int = 30,
    ): List<SoundcoreSong> {
        val candidates = mutableMapOf<String, SoundcoreSong>()
        val scoreMap = mutableMapOf<String, Double>()
        val artistFrequency = mutableMapOf<String, Int>()

        val normalizedSeedArtists = seedArtistNames.map { SoundcoreMatcher.normalize(it) }.filter { it.isNotBlank() }.toSet()

        // 1. Direct song seeds
        for (songId in seedSongIds.take(5)) {
            val recos = Soundcore.getRecommendations(songId)
            recos.forEachIndexed { index, song ->
                if (song.id !in excludeSongIds) {
                    candidates[song.id] = song
                    val rankBonus = (recos.size - index).toDouble() / recos.size.toDouble() * 5.0
                    scoreMap[song.id] = (scoreMap[song.id] ?: 0.0) + 10.0 + rankBonus
                }
            }
        }

        // 2. Artist seeds
        for (artist in seedArtistNames.take(4)) {
            val artistSongs = Soundcore.searchSongs(artist)
            artistSongs.take(8).forEachIndexed { index, song ->
                if (song.id !in excludeSongIds) {
                    candidates[song.id] = song
                    val rankBonus = (8 - index).toDouble() * 0.5
                    scoreMap[song.id] = (scoreMap[song.id] ?: 0.0) + 6.0 + rankBonus
                }
            }
        }

        // 3. Trending & Launch seeds
        val launchData = Soundcore.getLaunchData()
        launchData.trendingSongs.forEachIndexed { index, song ->
            if (song.id !in excludeSongIds) {
                candidates[song.id] = song
                val trendingBonus = (launchData.trendingSongs.size - index).toDouble() * 0.3
                scoreMap[song.id] = (scoreMap[song.id] ?: 0.0) + 4.0 + trendingBonus
            }
        }

        // 4. Score adjustment for artist affinity and diversity
        val scoredList = candidates.values.mapNotNull { song ->
            var finalScore = scoreMap[song.id] ?: return@mapNotNull null

            val artistText = song.artistsText
            if (!artistText.isNullOrBlank()) {
                val normArtist = SoundcoreMatcher.normalize(artistText)
                if (normalizedSeedArtists.any { normArtist.contains(it) || it.contains(normArtist) }) {
                    finalScore += 4.0
                }
            }

            val durationSeconds = SoundcoreMatcher.parseDurationSeconds(song.durationText)
            if (durationSeconds != null && (durationSeconds < 45 || durationSeconds > 900)) {
                finalScore -= 10.0
            }

            RecommendationScore(song, finalScore)
        }.sortedByDescending { it.score }

        // 5. Diversity selection (at most 3 songs per artist)
        val result = mutableListOf<SoundcoreSong>()
        for (item in scoredList) {
            val primaryArtist = item.song.artistsText?.split(",", "&")?.firstOrNull()?.trim() ?: "unknown"
            val count = artistFrequency[primaryArtist] ?: 0
            if (count < 3) {
                result.add(item.song)
                artistFrequency[primaryArtist] = count + 1
            }
            if (result.size >= limit) break
        }

        if (result.size < limit) {
            for (item in scoredList) {
                if (result.none { it.id == item.song.id }) {
                    result.add(item.song)
                }
                if (result.size >= limit) break
            }
        }

        return result
    }

    fun getSearchSuggestions(query: String): List<String> {
        val raw = Soundcore.getSearchSuggestions(query)
        if (raw.isEmpty()) return emptyList()

        val normalizedQuery = SoundcoreMatcher.normalize(query)
        return raw.distinctBy { SoundcoreMatcher.normalize(it) }
            .sortedByDescending { suggestion ->
                val normSugg = SoundcoreMatcher.normalize(suggestion)
                when {
                    normSugg.startsWith(normalizedQuery) -> 3
                    normSugg.contains(normalizedQuery) -> 2
                    else -> 1
                }
            }
            .take(10)
    }
}
