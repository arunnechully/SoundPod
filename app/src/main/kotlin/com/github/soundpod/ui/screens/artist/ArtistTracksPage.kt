package com.github.soundpod.ui.screens.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import com.github.innertube.Innertube
import com.github.innertube.requests.itemsPage
import com.github.innertube.requests.itemsPageContinuation
import com.github.innertube.utils.from
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.SortingHeader
import com.github.soundpod.ui.items.ListItemPlaceholder
import com.github.soundpod.ui.items.SongItem
import com.github.soundpod.ui.screens.search.ItemsPage
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@UnstableApi
@Composable
fun ArtistTracksPage(
    browseId: String,
    params: String? = null,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val emptyItemsText = stringResource(id = R.string.no_results_found)

    var sortBy by rememberSaveable { mutableStateOf(SongSortBy.Title) }
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.Ascending) }

    val tag = "artistSongs/$browseId/${params ?: ""}/list"
    
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.runtime.key(sortBy, sortOrder) {
            ItemsPage<Innertube.SongItem>(
                tag = tag,
                enableScrollbar = true,
                sortKeys = listOf(sortBy, sortOrder),
                sortTransform = { items ->
                    val comparator = when (sortBy) {
                        SongSortBy.Title -> compareBy<Innertube.SongItem> { it.info?.name?.lowercase() ?: "" }
                        SongSortBy.Artist -> compareBy<Innertube.SongItem> { it.authors?.mapNotNull { it.name }?.joinToString(" • ")?.lowercase() ?: "" }
                        else -> null
                    }
                    val sorted = if (comparator != null) items.sortedWith(comparator) else items
                    if (sortOrder == SortOrder.Descending) sorted.reversed() else sorted
                },
                header = { sortedItems ->
                    SortingHeader(
                        sortBy = sortBy,
                        changeSortBy = { sortBy = it },
                        sortByEntries = SongSortBy.entries.toList(),
                        sortOrder = sortOrder,
                        toggleSortOrder = { sortOrder = it },
                        onPlayClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(sortedItems.map { it.asMediaItem }, 0)
                        },
                        onShuffleClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(sortedItems.shuffled().map { it.asMediaItem }, 0)
                        }
                    )
                },
                itemsPageProvider = { continuation ->
                    if (continuation == null) {
                        Innertube.itemsPage(
                            browseId = browseId,
                            params = params,
                            fromMusicResponsiveListItemRenderer = { Innertube.SongItem.from(it) }
                        )
                    } else {
                        Innertube.itemsPageContinuation(
                            continuation = continuation,
                            fromMusicResponsiveListItemRenderer = { Innertube.SongItem.from(it) }
                        )
                    }
                },
                emptyItemsText = emptyItemsText,
                enablePreCache = true,
                itemContent = { song, index, sortedItems ->
                    SongItem(
                        song = song,
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(sortedItems.map { it.asMediaItem }, index)
                        },
                        onLongClick = {
                            menuState.display {
                                NonQueuedMediaItemMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = song.asMediaItem,
                                    onGoToAlbum = onAlbumClick,
                                    onGoToArtist = onArtistClick
                                )
                            }
                        }
                    )
                },
                itemPlaceholderContent = { ListItemPlaceholder() }
            )
        }
    }
}
