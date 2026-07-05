package com.github.soundpod.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.models.Song
import com.github.soundpod.models.SortBy
import com.github.soundpod.ui.items.LocalSongItem

@Composable
fun <T : SortBy> SongListContent(
    songs: List<Song>,
    sortBy: T,
    onSortByChange: (T) -> Unit,
    sortByEntries: List<T>,
    onSongClick: (Int) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    modifier: Modifier = Modifier,
    showThumbnail: Boolean = true,
    currentPlayingId : String? = null,
    leadingContent : (@Composable (index: Int, isPlaying: Boolean) -> Unit)? = null,
    showMoreVert: Boolean = true,
) {
    val playerPadding = LocalPlayerPadding.current
    val (colorPalette) = LocalAppearance.current
    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp + playerPadding),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "header") {
            SortingHeader(
                sortBy = sortBy,
                changeSortBy = onSortByChange,
                sortByEntries = sortByEntries,
                onPlayClick = onPlayAll,
                onShuffleClick = onShuffleAll
            )
        }
        itemsIndexed(
            songs, key = { _, song -> song.id }
        ) { index, song ->
            val isPlaying = song.id == currentPlayingId

            val highlightColor = if (isPlaying) {
                colorPalette.accent
            } else {
                colorPalette.text
            }
            LocalSongItem(
                song = song,
                showThumbnail = showThumbnail,
                titleColor = highlightColor,
                showMoreVert = showMoreVert,
                leadingContent = if (leadingContent != null) {
                    { leadingContent(index, isPlaying) }
                } else null,
                onClick = { onSongClick(index) },
                onLongClick = { onSongLongClick(song) }
            )
        }
    }
}