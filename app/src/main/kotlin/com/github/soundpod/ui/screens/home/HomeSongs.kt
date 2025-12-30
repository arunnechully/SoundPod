package com.github.soundpod.ui.screens.home

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Song
import com.github.soundpod.ui.components.SortingHeader
import com.github.soundpod.ui.components.InHistoryMediaItemMenu
import com.github.soundpod.ui.items.LocalSongItem
import com.github.soundpod.ui.styling.onOverlay
import com.github.soundpod.ui.styling.overlay
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.songSortByKey
import com.github.soundpod.utils.songSortOrderKey
import com.github.soundpod.viewmodels.home.HomeSongsViewModel

@OptIn(UnstableApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeSongs(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    var sortBy by rememberPreference(songSortByKey, SongSortBy.Title)
    var sortOrder by rememberPreference(songSortOrderKey, SortOrder.Ascending)

    val viewModel: HomeSongsViewModel = viewModel()

    LaunchedEffect(sortBy, sortOrder) {
        viewModel.loadSongs(
            sortBy = sortBy,
            sortOrder = sortOrder
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 400.dp),
        contentPadding = PaddingValues(bottom = if (viewModel.items.isNotEmpty()) 16.dp + 72.dp + playerPadding else 16.dp + playerPadding),
        modifier = Modifier.fillMaxSize()
    ) {
        item(
            key = "header",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            SortingHeader(
                sortBy = sortBy,
                changeSortBy = { sortBy = it },
                sortByEntries = SongSortBy.entries.toList(),
                sortOrder = sortOrder,
                toggleSortOrder = { sortOrder = !sortOrder },
                size = viewModel.items.size,
                itemCountText = R.plurals.number_of_songs
            )
        }

        itemsIndexed(
            items = viewModel.items,
            key = { _, song -> song.id }
        ) { index, song ->
            // REMOVED: SwipeToActionBox wrapper
            LocalSongItem(
                // Moved animateItem modifier here
                modifier = Modifier.animateItem(),
                song = song,
                onClick = {
                    binder?.stopRadio()
                    binder?.player?.forcePlayAtIndex(
                        viewModel.items.map(Song::asMediaItem),
                        index
                    )
                },
                onLongClick = {
                    menuState.display {
                        InHistoryMediaItemMenu(
                            song = song,
                            onDismiss = menuState::hide,
                            onGoToAlbum = onGoToAlbum,
                            onGoToArtist = onGoToArtist
                        )
                    }
                },
                onThumbnailContent = if (sortBy == SongSortBy.PlayTime) ({
                    Text(
                        text = song.formattedTotalPlayTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onOverlay,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.overlay
                                    )
                                ),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .align(Alignment.BottomCenter)
                    )
                }) else null
            )
        }
    }
}