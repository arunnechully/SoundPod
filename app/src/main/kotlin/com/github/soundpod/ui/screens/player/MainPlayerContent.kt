package com.github.soundpod.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.db
import com.github.soundpod.enums.PlayerLayout
import com.github.soundpod.enums.ProgressBar
import com.github.soundpod.ui.screens.player.lyrics.LyricsOverlay
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.isLandscape
import com.github.soundpod.utils.progressBarStyle
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.viewmodels.PlayerViewModel
import com.github.soundpod.viewmodels.PlaylistViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

@OptIn(UnstableApi::class)
@kotlin.OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun MainPlayerContent(
    expandProgress: Float,
    layoutMode: PlayerLayout,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onTrackDetailsClick: () -> Unit = {},
    onBack: () -> Unit,
    showPlaylist: Boolean,
    onSettingsClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onLyricsClick: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onTogglePlaylist: (Boolean) -> Unit,
    showLyrics: Boolean
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player

    val playlistViewModel = remember(player) { player?.let { PlaylistViewModel(it) } }
    val playerViewModel = remember(player) { player?.let { PlayerViewModel(it) } }

    val uiState by playerViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }

    val mediaItem = uiState?.mediaItem
    val artistId = uiState?.artistId
    val isSingleArtist = uiState?.isSingleArtist ?: false
    val albumId = uiState?.albumId
    val shouldBePlaying = (uiState?.playWhenReady ?: false) && uiState?.playbackState != Player.STATE_ENDED
    val currentPositionMs = uiState?.currentPositionMs ?: 0L
    val durationMs = uiState?.durationMs ?: 0L

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
    
    var isFullyCached by remember { mutableStateOf(false) }
    LaunchedEffect(mediaItem?.mediaId, binder) {
        val videoId = mediaItem?.mediaId ?: return@LaunchedEffect
        val cache = binder?.cache ?: return@LaunchedEffect
        
        combine(
            db.isDownloaded(videoId),
            binder.cacheChanges.onStart { emit(Unit) }
        ) { downloaded, _ ->
            if (downloaded) {
                (cache.getCachedBytes(videoId, 0, -1)) > 0L
            } else {
                false
            }
        }.distinctUntilChanged().collect {
            isFullyCached = it
        }
    }

    val progressBarStyleState = rememberPreference(progressBarStyle, ProgressBar.Default)
    val currentProgressStyle = progressBarStyleState.value

    val playingScale by animateFloatAsState(
        targetValue = if (shouldBePlaying) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "playingScale"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidth = maxWidth
        val thumbnailSize = containerWidth * 0.85f

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
            ) {
                // Left side: Overlay or Thumbnail spacer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (showPlaylist && playlistViewModel != null) {
                        PlaylistOverlay(
                            viewModel = playlistViewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 48.dp, horizontal = 12.dp),
                            onGoToAlbum = handleGoToAlbum,
                            onGoToArtist = handleGoToArtist
                        )
                    } else if (showLyrics && mediaItem != null) {
                        LyricsOverlay(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 48.dp, horizontal = 12.dp),
                            mediaId = mediaItem.mediaId,
                            mediaMetadata = mediaItem.mediaMetadata
                        )
                    }
                }

                // Right side: Controls
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayerTopControl(
                        onGoToAlbum = albumId?.let { id -> { handleGoToAlbum(id) } },
                        onGoToArtist = if (isSingleArtist && artistId != null) {
                            { handleGoToArtist(artistId) }
                        } else null,
                        onTrackDetailsClick = onTrackDetailsClick,
                        onLyricsClick = onLyricsClick,
                        onSettingsClick = onSettingsClick,
                        onSleepTimerClick = onSleepTimerClick,
                        onBack = {
                            if (showPlaylist) onTogglePlaylist(false) else onBack()
                        },
                        isPlaylistShowing = if (layoutMode == PlayerLayout.Default) showPlaylist || showLyrics else showPlaylist,
                        isFullyCached = isFullyCached
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            PlayerMediaItem(
                                onGoToArtist = if (isSingleArtist && artistId != null) {
                                    { handleGoToArtist(artistId) }
                                } else null
                            )

                            Spacer(modifier = Modifier.height(Dimensions.spacer))

                            if (layoutMode == PlayerLayout.Default) {
                                PlayerMiddleControl(
                                    showPlaylist = false,
                                    onTogglePlaylist = onTogglePlaylist,
                                    mediaId = mediaItem?.mediaId ?: "",
                                    onAddToPlaylist = onAddToPlaylist
                                )
                            } else {
                                PlayerControlBottom(
                                    shouldBePlaying = shouldBePlaying,
                                    onPlayPauseClick = { playerViewModel?.togglePlayPause() }
                                )
                            }
                        }
                    }

                    PlayerSeekBar(
                        mediaId = mediaItem?.mediaId ?: "",
                        position = currentPositionMs,
                        duration = durationMs,
                        progressBarStyle = currentProgressStyle,
                        isPlaying = shouldBePlaying,
                        onDraggingStateChange = { isDraggingSeekBar = it }
                    )

                    Spacer(modifier = Modifier.height(Dimensions.spacer))

                    if (layoutMode == PlayerLayout.Default) {
                        PlayerControlBottom(
                            shouldBePlaying = shouldBePlaying,
                            onPlayPauseClick = { playerViewModel?.togglePlayPause() }
                        )
                    } else {
                        PlayerMiddleControl(
                            showPlaylist = false,
                            onTogglePlaylist = onTogglePlaylist,
                            mediaId = mediaItem?.mediaId ?: "",
                            onAddToPlaylist = onAddToPlaylist
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimensions.spacer))
                }
            }
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
                    onGoToAlbum = albumId?.let { id -> { handleGoToAlbum(id) } },
                    onGoToArtist = if (isSingleArtist && artistId != null) {
                        { handleGoToArtist(artistId) }
                    } else null,
                    onTrackDetailsClick = onTrackDetailsClick,
                    onLyricsClick = onLyricsClick,
                    onSettingsClick = onSettingsClick,
                    onSleepTimerClick = onSleepTimerClick,
                    onBack = {
                        if (showPlaylist) onTogglePlaylist(false) else onBack()
                    },
                    isPlaylistShowing = if (layoutMode == PlayerLayout.Default) showPlaylist || showLyrics else showPlaylist,
                    isFullyCached = isFullyCached
                )

                Box(Modifier.weight(1f)) {
                    if (showPlaylist && playlistViewModel != null) {
//                        Column {
                            PlaylistOverlay(
                                viewModel = playlistViewModel,
//                                modifier = Modifier.weight(1f),
                                onGoToAlbum = handleGoToAlbum,
                                onGoToArtist = handleGoToArtist
                            )
//                            Spacer(modifier = Modifier.height(26.dp))
//                        }
                    } else if (showLyrics && mediaItem != null) {
//                        Column {
                            LyricsOverlay(
//                                modifier = Modifier.weight(1f),
                                mediaId = mediaItem.mediaId,
                                mediaMetadata = mediaItem.mediaMetadata
                            )
                            Spacer(modifier = Modifier.height(26.dp))
//                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Spacer(modifier = Modifier.height(Dimensions.spacer))

                            // Static placeholder for thumbnail area
                            Spacer(modifier = Modifier.size(thumbnailSize))

                            // Extra padding
                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        alpha = expandProgress.coerceIn(0f, 1f)

                                        val visualGap = (thumbnailSize.toPx() * (1f - playingScale)) / 2f
                                        translationY = -visualGap
                                    }
                            ) {
                                PlayerMediaItem(
                                    onGoToArtist = if (isSingleArtist && artistId != null) {
                                        { handleGoToArtist(artistId) }
                                    } else null
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
                                        mediaId = mediaItem?.mediaId ?: "",
                                        onAddToPlaylist = onAddToPlaylist
                                    )
                                } else {
                                    PlayerControlBottom(
                                        shouldBePlaying = shouldBePlaying,
                                        onPlayPauseClick = { playerViewModel?.togglePlayPause() }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                PlayerSeekBar(
                    mediaId = mediaItem?.mediaId ?: "",
                    position = currentPositionMs,
                    duration = durationMs,
                    progressBarStyle = currentProgressStyle,
                    isPlaying = shouldBePlaying,
                    onDraggingStateChange = { isDraggingSeekBar = it }
                )

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                if (layoutMode == PlayerLayout.Default) {
                    PlayerControlBottom(
                        shouldBePlaying = shouldBePlaying,
                        onPlayPauseClick = { playerViewModel?.togglePlayPause() }
                    )
                } else {
                    PlayerMiddleControl(
                        showPlaylist = false,
                        onTogglePlaylist = onTogglePlaylist,
                        mediaId = mediaItem?.mediaId ?: "",
                        onAddToPlaylist = onAddToPlaylist
                    )
                }
            }
        }
    }
}