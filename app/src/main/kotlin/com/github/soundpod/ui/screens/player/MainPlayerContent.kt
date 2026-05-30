package com.github.soundpod.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.enums.PlayerLayout
import com.github.soundpod.enums.ProgressBar
import com.github.soundpod.ui.screens.player.lyrics.LyricsOverlay
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.isLandscape
import com.github.soundpod.utils.progressBarStyle
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.viewmodels.PlayerViewModel // Ensure this is imported
import com.github.soundpod.viewmodels.PlaylistViewModel

@OptIn(UnstableApi::class)
@kotlin.OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun MainPlayerContent(
    layoutMode: PlayerLayout,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onBack: () -> Unit,
    showPlaylist: Boolean,
    onLyricsClick: () -> Unit = {},
    onTogglePlaylist: (Boolean) -> Unit,
    showLyrics: Boolean
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    val playlistViewModel = remember(player) { PlaylistViewModel(player) }

    // --- Initialize ViewModels ---
    val playerViewModel = remember(player) { PlayerViewModel(player) }

    // --- Observe UI State ---
    val uiState by playerViewModel.uiState.collectAsState()

    // --- Extract values safely ---
    val mediaItem = uiState.mediaItem ?: return
    val artistId = uiState.artistId
    val shouldBePlaying = uiState.isPlaying
    val currentPositionMs = uiState.currentPositionMs
    val durationMs = uiState.durationMs

    val handleGoToAlbum: (String) -> Unit = remember(onGoToAlbum, onBack) {
        { id -> onBack(); onGoToAlbum(id) }
    }

    val handleGoToArtist: (String) -> Unit = remember(onGoToArtist, onBack) {
        { id -> onBack(); onGoToArtist(id) }
    }

    BackHandler(enabled = true) {
        if (showPlaylist) onTogglePlaylist(false) else onBack()
    }

    var isDraggingSeekBar by remember { mutableStateOf(false) }

    val progressBarStyleState = rememberPreference(progressBarStyle, ProgressBar.Default)
    val currentProgressStyle = progressBarStyleState.value

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidth = maxWidth
        val thumbnailSize = containerWidth * 0.85f

        val textYOffset by animateDpAsState(
            targetValue = if (shouldBePlaying) 0.dp else -(thumbnailSize * 0.15f) / 2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "textYOffset"
        )

        if (isLandscape) {
            // TODO: Landscape implementation
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 48.dp
                    )
            ) {
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                //Top Control
                PlayerTopControl(
                    onGoToAlbum = handleGoToAlbum,
                    onGoToArtist = handleGoToArtist,
                    onLyricsClick = onLyricsClick,
                    onBack = {
                        if (showPlaylist) onTogglePlaylist(false) else onBack()
                    },
                    isPlaylistShowing = if (layoutMode == PlayerLayout.Default) showPlaylist || showLyrics else showPlaylist
                )

                Box(Modifier.weight(1f)) {
                    if (showPlaylist) {
                        Column {
                            PlaylistOverlay(
                                viewModel = playlistViewModel,
                                modifier = Modifier.weight(1f),
                                onGoToAlbum = handleGoToAlbum,
                                onGoToArtist = handleGoToArtist
                            )
                            Spacer(modifier = Modifier.height(26.dp))
                        }
                    } else if (showLyrics) {
                        Column {
                            LyricsOverlay(
                                modifier = Modifier.weight(1f),
                                mediaId = mediaItem.mediaId,
                                mediaMetadata = mediaItem.mediaMetadata,
                                currentPositionMs = currentPositionMs, // mapped from UI state
                                onSeekTo = { timeMs -> playerViewModel.seekTo(timeMs) } // mapped to ViewModel
                            )
                            Spacer(modifier = Modifier.height(26.dp))
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Spacer(modifier = Modifier.size(thumbnailSize))

                            Box(modifier = Modifier.offset(y = textYOffset)) {
                                PlayerMediaItem(
                                    onGoToArtist = artistId?.let { artist -> { handleGoToArtist(artist) } }
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            val middleControlAlpha by animateFloatAsState(
                                targetValue = if (isDraggingSeekBar) 0f else 1f,
                                animationSpec = tween(durationMillis = 300),
                                label = "MiddleControlFade"
                            )

                            Box(modifier = Modifier.graphicsLayer { alpha = middleControlAlpha }) {
                                if (layoutMode == PlayerLayout.Default) {
                                    PlayerMiddleControl(
                                        showPlaylist = false,
                                        onTogglePlaylist = onTogglePlaylist,
                                        mediaId = mediaItem.mediaId
                                    )
                                } else {
                                    PlayerControlBottom(
                                        shouldBePlaying = shouldBePlaying,
                                        onPlayPauseClick = { playerViewModel.togglePlayPause() } // mapped to ViewModel
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                PlayerSeekBar(
                    mediaId = mediaItem.mediaId,
                    position = currentPositionMs, // mapped from UI state
                    duration = durationMs, // mapped from UI state
                    progressBarStyle = currentProgressStyle,
                    onDraggingStateChange = { isDraggingSeekBar = it }
                )

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                if (layoutMode == PlayerLayout.Default) {
                    PlayerControlBottom(
                        shouldBePlaying = shouldBePlaying,
                        onPlayPauseClick = { playerViewModel.togglePlayPause() } // mapped to ViewModel
                    )
                } else {
                    PlayerMiddleControl(
                        showPlaylist = false,
                        onTogglePlaylist = onTogglePlaylist,
                        mediaId = mediaItem.mediaId
                    )
                }
            }
        }
    }
}