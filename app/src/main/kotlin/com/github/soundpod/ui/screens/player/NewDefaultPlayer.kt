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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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

@OptIn(UnstableApi::class)
@kotlin.OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun NewMainPlayerContent(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onBack: () -> Unit,
    showPlaylist: Boolean,
    onTogglePlaylist: (Boolean) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    var nullableMediaItem by remember {
        mutableStateOf(player.currentMediaItem, neverEqualPolicy())
    }
    val mediaItem = nullableMediaItem ?: return

    var artistId: String? by remember(mediaItem) {
        mutableStateOf(
            mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let {
                if (it.size == 1) it.first() else null
            }
        )
    }

    var shouldBePlaying by remember { mutableStateOf(player.shouldBePlaying) }

    val handleGoToAlbum: (String) -> Unit = remember(onGoToAlbum, onBack) {
        { id ->
            onBack()
            onGoToAlbum(id)
        }
    }

    val handleGoToArtist: (String) -> Unit = remember(onGoToArtist, onBack) {
        { id ->
            onBack()
            onGoToArtist(id)
        }
    }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                @Suppress("AssignedValueIsNeverRead")
                nullableMediaItem = mediaItem
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = player.shouldBePlaying
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = player.shouldBePlaying
            }
        }
    }

    LaunchedEffect(mediaItem) {
        if (artistId == null) {
            withContext(Dispatchers.IO) {
                val artistsInfo = db.songArtistInfo(mediaItem.mediaId)
                if (artistsInfo.size == 1) {
                    artistId = artistsInfo.first().id
                }
            }
        }
    }

    BackHandler(enabled = true) {
        if (showPlaylist) onTogglePlaylist(false) else onBack()
    }
    var isDraggingSeekBar by remember { mutableStateOf(false) }

    val positionAndDuration by player.positionAndDurationState()
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

                PlayerTopControl(
                    onGoToAlbum = handleGoToAlbum,
                    onGoToArtist = handleGoToArtist,
                    onBack = {
                        if (showPlaylist) onTogglePlaylist(false) else onBack()
                    },
                    isPlaylistShowing = showPlaylist
                )

                Box(Modifier.weight(1f)) {
                    if (showPlaylist) {
                        Column {
                            PlaylistOverlay(
                                modifier = Modifier.weight(1f),
                                onGoToAlbum = handleGoToAlbum,
                                onGoToArtist = handleGoToArtist
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
                                    onGoToArtist = artistId?.let { artist ->
                                        { handleGoToArtist(artist) }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            val middleControlAlpha by animateFloatAsState(
                                targetValue = if (isDraggingSeekBar) 0f else 1f,
                                animationSpec = tween(durationMillis = 300),
                                label = "MiddleControlFade"
                            )
                            Box(modifier = Modifier.graphicsLayer { alpha = middleControlAlpha }) {
                                PlayerMiddleControl(
                                    showPlaylist = false,
                                    onTogglePlaylist = onTogglePlaylist,
                                    mediaId = mediaItem.mediaId
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                PlayerSeekBar(
                    mediaId = mediaItem.mediaId,
                    position = positionAndDuration.first,
                    duration = positionAndDuration.second,
                    progressBarStyle = currentProgressStyle,
                    onDraggingStateChange = { isDraggingSeekBar = it }
                )

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                PlayerControlBottom(
                    shouldBePlaying = shouldBePlaying,
                    onPlayPauseClick = {
                        if (shouldBePlaying) {
                            player.pause()
                        } else {
                            when (player.playbackState) {
                                Player.STATE_IDLE -> player.prepare()
                                Player.STATE_ENDED -> player.seekToDefaultPosition(0)
                                Player.STATE_BUFFERING,
                                Player.STATE_READY -> {
                                }
                            }
                            player.play()
                        }
                    }
                )
            }
        }
    }
}