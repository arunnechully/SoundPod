package com.github.soundpod.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.StaticScreenLayout
import com.github.soundpod.ui.items.LocalSongItem
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.viewmodels.home.HistoryViewModel

@OptIn(ExperimentalAnimationApi::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun HistoryScreen(
    onBackClick: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val playerPadding = LocalPlayerPadding.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    StaticScreenLayout(
        title = stringResource(id = R.string.history),
        onBackClick = onBackClick,
        scrollable = false,
        horizontalPadding = 0.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = playerPadding + 16.dp)
        ) {
            items(items = viewModel.historySongs, key = { it.id }) { song ->
                LocalSongItem(
                    song = song,
                    onClick = {
                        val mediaItem = song.asMediaItem
                        binder?.stopRadio()
                        binder?.player?.setMediaItem(mediaItem)
                        binder?.player?.prepare()
                        binder?.player?.play()
                    },
                    onLongClick = {
                        menuState.display {
                            NonQueuedMediaItemMenu(
                                onDismiss = menuState::hide,
                                mediaItem = song.asMediaItem,
                                onGoToAlbum = onGoToAlbum,
                                onGoToArtist = onGoToArtist
                            )
                        }
                    }
                )
            }
        }
    }
}
