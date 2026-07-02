package com.github.soundpod.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.enums.PlaylistSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.Playlist
import com.github.soundpod.query
import com.github.soundpod.ui.components.SortingHeader
import com.github.soundpod.ui.components.TextFieldDialog
import com.github.soundpod.ui.screens.favorites.FavoritesCard
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayFromBeginning
import com.github.soundpod.utils.playlistSortByKey
import com.github.soundpod.utils.playlistSortOrderKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.showCachedSongsInOfflineKey
import com.github.soundpod.viewmodels.home.HomePlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun HomePlaylists(
    onBuiltInPlaylist: (Int) -> Unit,
    onPlaylistClick: (Playlist) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val playerPadding = LocalPlayerPadding.current

    var isCreatingANewPlaylist by rememberSaveable { mutableStateOf(false) }
    var sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.Name)
    var sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Ascending)
    val showCachedSongsInOffline by rememberPreference(showCachedSongsInOfflineKey, true)

    var offlineThumbnailPool by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentOfflineThumbnail by rememberSaveable { mutableStateOf<String?>(null) }

    var hasOfflineSongs by rememberSaveable { mutableStateOf(false) }

    val viewModel: HomePlaylistsViewModel = viewModel()

    LaunchedEffect(binder, showCachedSongsInOffline) {
        db.songsWithContentLength()
            .map { songsWithLength ->
                songsWithLength
                    .filterNot { it.song.id.startsWith("content://") || it.song.id.startsWith("file://") }
                    .filter { item ->
                        item.isDownloaded || (showCachedSongsInOffline && binder?.isCached(item.song.id, 0, item.contentLength ?: -1L) == true)
                    }
            }
            .distinctUntilChanged()
            .collect { filteredSongs ->
                offlineThumbnailPool = filteredSongs.mapNotNull { it.song.thumbnailUrl }
                hasOfflineSongs = filteredSongs.isNotEmpty()
            }
    }

    LaunchedEffect(offlineThumbnailPool) {
        if (offlineThumbnailPool.isNotEmpty()) {
            if (currentOfflineThumbnail == null || currentOfflineThumbnail !in offlineThumbnailPool) {
                currentOfflineThumbnail = offlineThumbnailPool.random()
            }
            while (true) {
                delay(5000.milliseconds)
                if (offlineThumbnailPool.size > 1) {
                    var nextThumbnail = offlineThumbnailPool.random()
                    while (nextThumbnail == currentOfflineThumbnail && offlineThumbnailPool.size > 1) {
                        nextThumbnail = offlineThumbnailPool.random()
                    }
                    currentOfflineThumbnail = nextThumbnail
                }
            }
        } else {
            currentOfflineThumbnail = null
        }
    }

    LaunchedEffect(sortBy, sortOrder) {
        viewModel.loadArtists(
            sortBy = sortBy,
            sortOrder = sortOrder
        )
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
            start = 8.dp,
            end = 8.dp,
            bottom = 16.dp + playerPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(
            key = "header",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            SortingHeader(
                sortBy = sortBy,
                changeSortBy = { sortBy = it },
                sortByEntries = PlaylistSortBy.entries.toList(),
                sortOrder = sortOrder,
                toggleSortOrder = { sortOrder = !sortOrder },
                size = viewModel.items.size,
                itemCountText = R.plurals.number_of_playlists
            )
        }

        if (hasOfflineSongs) {
            item(key = "offline") {
                FavoritesCard(
                    title = stringResource(id = R.string.offline),
                    icon = Icons.Default.DownloadForOffline,
                    thumbnailUrls = listOfNotNull(currentOfflineThumbnail),
                    onClick = { onBuiltInPlaylist(BuiltInPlaylist.Offline.ordinal) },
                    onPlayClick = {
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            val offlineSongs = if (showCachedSongsInOffline) {
                                db.songsWithContentLength().first()
                                    .filterNot { it.song.id.startsWith("content://") || it.song.id.startsWith("file://") }
                                    .filter { item ->
                                        item.isDownloaded || (binder?.isCached(item.song.id, 0, item.contentLength ?: -1L) == true)
                                    }.map { it.song }
                            } else {
                                db.downloadedSongsWithContentLength()
                                    .first()
                                    .map { it.song }
                            }

                            launch(Dispatchers.Main) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(offlineSongs.map { it.asMediaItem })
                            }
                        }
                    }
                )
            }
        }

        item(key = "new") {
            FavoritesCard(
                title = stringResource(id = R.string.create_new_playlist),
                onClick = { isCreatingANewPlaylist = true }
            )
        }

        items(
            items = viewModel.items,
            key = { it.playlist.id }
        ) { playlistPreview ->
            val thumbnails by remember(playlistPreview.playlist.id) {
                db.playlistThumbnailUrls(playlistPreview.playlist.id)
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.IO)
            }.collectAsState(initial = emptyList())

            FavoritesCard(
                modifier = Modifier.animateItem(),
                title = playlistPreview.playlist.name,
                subtitle = pluralStringResource(
                    id = R.plurals.number_of_songs,
                    count = playlistPreview.songCount,
                    playlistPreview.songCount
                ),
                thumbnailUrls = thumbnails,
                onClick = { onPlaylistClick(playlistPreview.playlist) },
                onPlayClick = {
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val songs = db.playlistSongs(playlistPreview.playlist.id).first()
                        launch(Dispatchers.Main) {
                            binder?.stopRadio()
                            binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem })
                        }
                    }
                }
            )
        }
    }
}