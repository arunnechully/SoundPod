package com.github.soundpod.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PrecachedSong(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis()
)
