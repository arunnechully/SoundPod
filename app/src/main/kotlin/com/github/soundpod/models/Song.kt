package com.github.soundpod.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artistsText: String? = null,
    val durationText: String?,
    val thumbnailUrl: String?,
    val likedAt: Long? = null,
    val totalPlayTimeMs: Long = 0,
    val blacklistedAt: Long? = null
) {
    val formattedTotalPlayTime: String
        get() {
            val seconds = totalPlayTimeMs / 1000

            val hours = seconds / 3600

            return when {
                hours == 0L -> "${seconds / 60}m"
                hours < 24L -> "${hours}h"
                else -> "${hours / 24}d"
            }
        }

    fun toggleLike(): Song {
        val newLikedAt = if (likedAt == null) System.currentTimeMillis() else null
        return copy(
            likedAt = newLikedAt,
            blacklistedAt = if (newLikedAt != null) null else blacklistedAt
        )
    }

    fun toggleBlacklist(): Song {
        val newBlacklistedAt = if (blacklistedAt == null) System.currentTimeMillis() else null
        return copy(
            blacklistedAt = newBlacklistedAt,
            likedAt = if (newBlacklistedAt != null) null else likedAt
        )
    }
}
