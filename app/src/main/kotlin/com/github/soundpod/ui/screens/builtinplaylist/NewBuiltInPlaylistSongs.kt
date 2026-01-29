package com.github.soundpod.ui.screens.builtinplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.db
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Song
import com.github.soundpod.models.SongWithContentLength
import com.github.soundpod.ui.components.InHistoryMediaItemMenu
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.items.LocalSongItem
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun NewBuiltInPlaylistSongs(
    builtInPlaylist: BuiltInPlaylist,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    var songs: List<Song> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(builtInPlaylist) {
        when (builtInPlaylist) {
            BuiltInPlaylist.Favorites -> db.favorites()
            BuiltInPlaylist.Offline -> db
                .songsWithContentLength()
                .flowOn(Dispatchers.IO)
                .map { songWithLengths ->
                    songWithLengths.filter { item ->
                        item.contentLength?.let {
                            binder?.cache?.isCached(item.song.id, 0, it)
                        } ?: false
                    }.map(SongWithContentLength::song)
                }
        }.collect { songs = it }
    }
        LazyColumn(
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + playerPadding),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                LocalSongItem(
                    song = song,
                    onClick = {
                        binder?.stopRadio()
                        binder?.player?.forcePlayAtIndex(
                            songs.map(Song::asMediaItem),
                            index
                        )
                    },
                    onLongClick = {
                        menuState.display {
                            when (builtInPlaylist) {
                                BuiltInPlaylist.Favorites ->
                                    NonQueuedMediaItemMenu(
                                        mediaItem = song.asMediaItem,
                                        onDismiss = menuState::hide,
                                        onGoToAlbum = onGoToAlbum,
                                        onGoToArtist = onGoToArtist
                                    )

                                BuiltInPlaylist.Offline ->
                                    InHistoryMediaItemMenu(
                                        song = song,
                                        onDismiss = menuState::hide,
                                        onGoToAlbum = onGoToAlbum,
                                        onGoToArtist = onGoToArtist
                                    )
                            }
                        }
                    }
                )
            }
        }
}
