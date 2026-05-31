package com.github.soundpod.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.NavController
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.ui.appearance.PlayerBackground
import com.github.soundpod.ui.navigation.Routes
import com.github.soundpod.utils.isLandscape
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedPlayer(
    navController: NavController,
    onNavigateToSettings: () -> Unit,
    sheetState: SheetState,
    scaffoldPadding: PaddingValues,
    showPlayer: Boolean,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current

    var showPlaylist by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var targetExpandProgress by remember { mutableFloatStateOf(0f) }

    val expandProgress by animateFloatAsState(
        targetValue = targetExpandProgress,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessLow
        ),
        label = "expandAnimation"
    )

    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player
    var currentArtworkUrl by remember {
        mutableStateOf(player?.currentMediaItem?.mediaMetadata?.artworkUri?.toString())
    }

    var hasMediaItems by remember {
        mutableStateOf((player?.mediaItemCount ?: 0) > 0)
    }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}

        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val metadata = mediaItem?.mediaMetadata
                currentArtworkUrl = metadata?.artworkUri?.toString()
                    ?: metadata?.extras?.getString("artwork_url")
                    ?: ""
                hasMediaItems = player.mediaItemCount > 0
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                hasMediaItems = player.mediaItemCount > 0
            }
        }
        player.addListener(listener)

        hasMediaItems = player.mediaItemCount > 0
        currentArtworkUrl = player.currentMediaItem?.mediaMetadata?.artworkUri?.toString()
            ?: player.currentMediaItem?.mediaMetadata?.extras?.getString("artwork_url")
            ?: ""

        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(expandProgress) {
        if (expandProgress == 0f) {
            showLyrics = false
            showPlaylist = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val exactScreenHeight = maxHeight
        val exactScreenWidth = maxWidth
        val screenHeightPx = with(density) { exactScreenHeight.toPx() }

        val systemBottomPadding = scaffoldPadding.calculateBottomPadding()
        val activeBottomPadding = lerp(systemBottomPadding, 0.dp, expandProgress).coerceAtLeast(0.dp)
        val playerHeight = lerp(60.dp, exactScreenHeight, expandProgress).coerceAtLeast(0.dp)

        val cornerRadius = lerp(28.dp, 0.dp, expandProgress).coerceAtLeast(0.dp)

        CompositionLocalProvider(value = LocalPlayerPadding provides (60.dp + systemBottomPadding)) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(
                    start = scaffoldPadding.calculateLeftPadding(layoutDirection),
                    end = scaffoldPadding.calculateRightPadding(layoutDirection)
                ),
                content = content
            )
        }

        if (showPlayer) {
            val dragGestureModifier = if (showPlaylist || showLyrics || !hasMediaItems) {
                Modifier
            } else {
                Modifier.pointerInput(screenHeightPx) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Higher multiplier for even lighter feel (3x)
                            val delta = (dragAmount / screenHeightPx) * 3f
                            targetExpandProgress = (targetExpandProgress - delta).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            // Even more generous snapping (20% threshold)
                            targetExpandProgress = if (targetExpandProgress > 0.2f) 1f else 0f
                            scope.launch {
                                if (targetExpandProgress == 1f) sheetState.expand() else sheetState.partialExpand()
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = activeBottomPadding)
                    .width(exactScreenWidth)
                    .height(playerHeight)
                    .graphicsLayer {
                        shape = RoundedCornerShape(cornerRadius)
                        clip = true
                    }
                    .then(dragGestureModifier)
            ) {
                PlayerBackground(thumbnailUrl = currentArtworkUrl) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        if (expandProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .layout { measurable, constraints ->
                                        val screenW = exactScreenWidth.roundToPx()
                                        val screenH = exactScreenHeight.roundToPx()

                                        //The actual size the parent box currently is
                                        val currentH = constraints.maxHeight
                                        val pad = activeBottomPadding.roundToPx()

                                        //Measure the inner player at FULL screen size so it stays static
                                        val placeable = measurable.measure(
                                            androidx.compose.ui.unit.Constraints.fixed(screenW, screenH)
                                        )

                                        // This ensures SharedThumbnail isn't pushed off the screen!
                                        layout(constraints.maxWidth, currentH) {

                                            //Shift the inner player UP to cancel out the parent's movement
                                            val yOffset = currentH + pad - screenH
                                            placeable.placeRelative(0, yOffset)
                                        }
                                    }
                                    .alpha(expandProgress)
                            ) {
                                PlayerLayout(
                                    expandProgress = expandProgress,
                                    onGoToAlbum = { browseId ->
                                        scope.launch { sheetState.partialExpand() }
                                        navController.navigate(route = Routes.Album(id = browseId))
                                    },
                                    onGoToArtist = { browseId ->
                                        scope.launch { sheetState.partialExpand() }
                                        navController.navigate(route = Routes.Artist(id = browseId))
                                    },
                                    onGoToTrackDetails = { navController.navigate(route = Routes.TrackDetails)},
                                    onBack = {
                                        if (showLyrics) {
                                            showLyrics = false
                                        } else if (showPlaylist) {
                                            showPlaylist = false
                                        } else {
                                            targetExpandProgress = 0f
                                            scope.launch { sheetState.partialExpand() }
                                        }
                                    },
                                    showPlaylist = showPlaylist,
                                    onLyricsClick = {
                                        showLyrics = true
                                        showPlaylist = false
                                    },
                                    showLyrics = showLyrics,
                                    onSettingsClick = onNavigateToSettings,
                                    onTogglePlaylist = {
                                        showPlaylist = it
                                        if (it) showLyrics = false
                                    }
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = !showPlaylist && !showLyrics,
                            enter = fadeIn(tween(400)),
                            exit = fadeOut(tween(400))
                        ) {
                            SharedThumbnail(
                                expandProgress = expandProgress,
                                isLandscape = isLandscape
                            )
                        }
                    }
                }
            }

            if (expandProgress < 1f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            shape = RoundedCornerShape(cornerRadius)
                            clip = true
                        }
                        .align(Alignment.BottomCenter)
                        .padding(bottom = systemBottomPadding)
                        .alpha(1f - expandProgress)
                        .then(dragGestureModifier)
                ){
                    MiniPlayerContent(
                        openPlayer = {
                            targetExpandProgress = 1f
                            scope.launch { sheetState.expand() }
                        }
                    )
                }
            }
        }
    }
}