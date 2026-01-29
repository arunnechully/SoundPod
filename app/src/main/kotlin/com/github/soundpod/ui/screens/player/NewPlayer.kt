package com.github.soundpod.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.db
import com.github.soundpod.enums.ProgressBar
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.isLandscape
import com.github.soundpod.utils.positionAndDurationState
import com.github.soundpod.utils.progressBarStyle
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.shouldBePlaying
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun NewPlayer(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return
    var nullableMediaItem by remember {
        mutableStateOf(
            binder.player.currentMediaItem,
            neverEqualPolicy()
        )
    }

    val mediaItem = nullableMediaItem ?: return

    var artistId: String? by remember(mediaItem) {
        mutableStateOf(
            mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { artists ->
                if (artists.size == 1) artists.first()
                else null
            }
        )
    }

    var shouldBePlaying by remember { mutableStateOf(binder.player.shouldBePlaying) }

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    LaunchedEffect(mediaItem) {
        withContext(Dispatchers.IO) {
            if (artistId == null) {
                val artistsInfo = db.songArtistInfo(mediaItem.mediaId)
                if (artistsInfo.size == 1) artistId = artistsInfo.first().id
            }
        }
    }

    var showPlaylist by remember { mutableStateOf(false) }

    BackHandler(enabled = showPlaylist) {
        showPlaylist = false
    }

    val positionAndDuration by binder.player.positionAndDurationState()

    var isShowingLyrics by rememberSaveable { mutableStateOf(false) }
    var fullScreenLyrics by remember { mutableStateOf(false) }
    var isShowingStatsForNerds by rememberSaveable { mutableStateOf(false) }

    val progressBarStyleState = rememberPreference(progressBarStyle, ProgressBar.Default)
    val progressBarStyle = progressBarStyleState.value


    if (isLandscape) {
        //todo
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    windowInsets
                        .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                )
                .padding(bottom = 60.dp)
        ) {

            Spacer(modifier = Modifier.height(Dimensions.spacer))

            PlayerTopControl(
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onBack = {},
            )

            Box(Modifier.weight(1f)) {
                if (showPlaylist) {
                    // Show PlaylistOverlay when playlist is visible
                    Column {
                        PlaylistOverlay(
                            modifier = Modifier.weight(1f),
                            onGoToAlbum = onGoToAlbum,
                            onGoToArtist = onGoToArtist
                        )
                        Spacer(modifier = Modifier.height(26.dp))
                    }
                } else {
                    // Show thumbnail and media info when playlist is hidden
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Spacer(modifier = Modifier.height(30.dp))

                        NewThumbnail(
                            isShowingLyrics = isShowingLyrics,
                            onShowLyrics = { isShowingLyrics = it },
                            fullScreenLyrics = fullScreenLyrics,
                            toggleFullScreenLyrics = { fullScreenLyrics = !fullScreenLyrics },
                            isShowingStatsForNerds = isShowingStatsForNerds,
                            onShowStatsForNerds = { isShowingStatsForNerds = it },
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth(),
                            mediaId = mediaItem.mediaId
                        )

                        Spacer(modifier = Modifier.padding(vertical = 5.dp))

                        PlayerMediaItem(
                            onGoToArtist = artistId?.let {
                                { onGoToArtist(it) }
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        PlayerMiddleControl(
                            showPlaylist = showPlaylist,
                            onTogglePlaylist = { showPlaylist = it },
                            mediaId = mediaItem.mediaId
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spacer))

            PlayerSeekBar(
                mediaId = mediaItem.mediaId,
                position = positionAndDuration.first,
                duration = positionAndDuration.second,
                progressBarStyle = progressBarStyle
            )

            Spacer(modifier = Modifier.height(Dimensions.spacer))

            PlayerControlBottom(
                shouldBePlaying = shouldBePlaying,
                onPlayPauseClick = {
                    if (shouldBePlaying) {
                        binder.player.pause()
                    } else {
                        if (binder.player.playbackState == Player.STATE_IDLE) {
                            binder.player.prepare()
                        } else if (binder.player.playbackState == Player.STATE_ENDED) {
                            binder.player.seekToDefaultPosition(0)
                        }
                        binder.player.play()
                    }
                }
            )
        }

    }
}