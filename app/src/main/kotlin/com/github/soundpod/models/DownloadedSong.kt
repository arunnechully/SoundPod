package com.github.soundpod.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity
data class DownloadedSong(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis()
)
