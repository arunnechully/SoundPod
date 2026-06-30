package com.github.soundpod.models

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Immutable
@Entity
class Lyrics(
    @PrimaryKey val songId: String,
    val fixed: String?,
    val synced: String?,
)
