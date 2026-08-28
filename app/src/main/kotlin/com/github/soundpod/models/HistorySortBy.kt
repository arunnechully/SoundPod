package com.github.soundpod.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Title
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.soundpod.R

enum class HistorySortBy(
    override val text: Int,
    override val icon: ImageVector
) : SortBy {
    Recent(R.string.recent, Icons.Default.History),
    Title(R.string.title, Icons.Default.Title),
    Artist(R.string.artist, Icons.Default.Person),
    Custom(R.string.custom, Icons.AutoMirrored.Filled.Sort)
}
