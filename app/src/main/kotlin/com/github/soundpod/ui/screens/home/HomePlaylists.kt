package com.github.soundpod.ui.screens.home

import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.enums.PlaylistSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.Playlist
import com.github.soundpod.query
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.FavoritesCard
import com.github.soundpod.ui.components.TextFieldDialog
import com.github.soundpod.utils.playlistSortByKey
import com.github.soundpod.utils.playlistSortOrderKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.viewmodels.home.HomePlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@OptIn(UnstableApi::class)
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun HomePlaylists(
    onBuiltInPlaylist: (Int) -> Unit,
    onPlaylistClick: (Playlist) -> Unit
) {
    @OptIn(UnstableApi::class)
    val playerPadding = LocalPlayerPadding.current
    val binder = LocalPlayerServiceBinder.current

    var isCreatingANewPlaylist by rememberSaveable { mutableStateOf(false) }
    var sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.Name)
    var sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Ascending)

    val viewModel: HomePlaylistsViewModel = viewModel()

    LaunchedEffect(sortBy, sortOrder) {
        viewModel.loadPlaylists(
            sortBy = sortBy,
            sortOrder = sortOrder
        )
    }

    LaunchedEffect(binder) {
        db.songsWithContentLength()
            .map { songsWithLength ->
                val binderCache = binder?.cache
                songsWithLength
                    .filterNot {
                        it.song.id.startsWith("content://") || it.song.id.startsWith("file://")
                    }.filter { item ->
                        val length = item.contentLength
                        if (length != null) {
                            binderCache?.isCached(item.song.id, 0, length) == true
                        } else {
                            (binderCache?.getCachedBytes(item.song.id, 0, -1) ?: 0L) > 0L
                        }
                    }.shuffled().firstOrNull()?.song?.thumbnailUrl
            }
            .flowOn(Dispatchers.IO)
            .collect {
                viewModel.offlineThumbnail = it
            }
    }

    val playlistName = stringResource(R.string.playlist)

    if (isCreatingANewPlaylist) {
        TextFieldDialog(
            title = stringResource(id = R.string.new_playlist),
            hintText = stringResource(id = R.string.playlist_name_hint),
            initialTextInput = "$playlistName %03d".format(viewModel.items.size + 1),
            onDismiss = {
                isCreatingANewPlaylist = false
            },
            onDone = { text ->
                query {
                    db.insert(Playlist(name = text))
                }
                isCreatingANewPlaylist = false
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            start = 8.dp,
            end = 8.dp,
            bottom = playerPadding + 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "offline") {
            FavoritesCard(
                title = stringResource(id = R.string.offline),
                icon = IconSource.Icon(painterResource(id = R.drawable.offline_music)),
                thumbnailUrls = listOfNotNull(viewModel.offlineThumbnail),
                onClick = { onBuiltInPlaylist(BuiltInPlaylist.Offline.ordinal) }
            )
        }
        item(key = "new") {
            FavoritesCard(
//                title = stringResource(id = R.string.create_new_playlist),
                icon = IconSource.Icon( painterResource(id = R.drawable.add)),
                onClick = { isCreatingANewPlaylist = true }
            )
        }

        items(
            items = viewModel.items,
            key = { it.playlist.id }
        ) { playlistPreview ->
            FavoritesCard(
                modifier = Modifier.animateItem(),
                title = playlistPreview.playlist.name,
                thumbnailUrls = playlistPreview.thumbnails.filterNotNull(),
                subtitle = pluralStringResource(
                    id = R.plurals.number_of_songs,
                    count = playlistPreview.songCount,
                    playlistPreview.songCount
                ),
                onClick = { onPlaylistClick(playlistPreview.playlist) }
            )
        }
    }
}