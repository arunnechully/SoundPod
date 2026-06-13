package com.github.soundpod.ui.screens.player

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.SettingsActivity
import com.github.soundpod.ui.appearance.PlayerBackground
import com.github.soundpod.ui.navigation.Routes
import com.github.soundpod.ui.navigation.SettingsDestinations
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
    onNavigateToSleepTimer: () -> Unit,
    sheetState: SheetState,
    scaffoldPadding: PaddingValues,
    showPlayer: Boolean,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current

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

    var isPlaying by remember { mutableStateOf(player?.isPlaying ?: false) }

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

            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
        }
        player.addListener(listener)

        hasMediaItems = player.mediaItemCount > 0
        isPlaying = player.isPlaying
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = scaffoldPadding.calculateLeftPadding(layoutDirection),
                        end = scaffoldPadding.calculateRightPadding(layoutDirection)
                    ),
                content = content
            )
        }

        if (showPlayer) {
            val (colorPalette) = LocalAppearance.current
            val isDark = colorPalette.isDark
            val bottomPaddingColor = if (isDark) Color.Black else Color.White

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(activeBottomPadding + 30.dp)
                    .background(bottomPaddingColor)
                    .alpha(if (expandProgress > 0f) 0f else 1f)
            )

            val dragGestureModifier = if (showPlaylist || showLyrics || !hasMediaItems) {
                Modifier
            } else {
                Modifier.pointerInput(screenHeightPx) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = (dragAmount / screenHeightPx) * 3f
                            targetExpandProgress = (targetExpandProgress - delta).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
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
                PlayerBackground(
                    thumbnailUrl = currentArtworkUrl,
                    isPlaying = isPlaying,
                    expandProgress = expandProgress
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        if (expandProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .layout { measurable, constraints ->
                                        val screenW = exactScreenWidth.roundToPx()
                                        val screenH = exactScreenHeight.roundToPx()
                                        val currentH = constraints.maxHeight
                                        val pad = activeBottomPadding.roundToPx()

                                        val placeable = measurable.measure(
                                            androidx.compose.ui.unit.Constraints.fixed(screenW, screenH)
                                        )

                                        layout(constraints.maxWidth, currentH) {
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
                                        navController.navigate(route = Routes.Album(id = browseId)) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onGoToArtist = { browseId ->
                                        scope.launch { sheetState.partialExpand() }
                                        navController.navigate(route = Routes.Artist(id = browseId)) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onGoToTrackDetails = {
                                        val intent = Intent(context, SettingsActivity::class.java).apply {
                                            putExtra("SCREEN_ID", SettingsDestinations.TRACK_DETAILS)
                                        }
                                        context.startActivity(intent)
                                    },
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
                                    onSleepTimerClick = onNavigateToSleepTimer,
                                    onTogglePlaylist = {
                                        showPlaylist = it
                                        if (it) showLyrics = false
                                    }
                                )
                            }
                        }

                        if (expandProgress < 1f) {
                            Box(
                                modifier = Modifier
                                    .layout { measurable, constraints ->
                                        val screenW = exactScreenWidth.roundToPx()
                                        val currentH = constraints.maxHeight
                                        val pad = activeBottomPadding.roundToPx()
                                        val sysPad = systemBottomPadding.roundToPx()
                                        val miniH = 60.dp.roundToPx()

                                        val placeable = measurable.measure(
                                            androidx.compose.ui.unit.Constraints.fixed(screenW, miniH)
                                        )

                                        layout(constraints.maxWidth, currentH) {
                                            // Pin it relative to screen bottom:
                                            // The parent Box bottom is at screenH - pad.
                                            // We want mini player bottom at screenH - sysPad.
                                            // Offset from parent bottom = sysPad - pad.
                                            // Offset from parent top = currentH - miniH - (sysPad - pad)
                                            val yOffset = currentH - miniH - (sysPad - pad)
                                            placeable.placeRelative(0, yOffset)
                                        }
                                    }
                                    .alpha(1f - expandProgress)
                            ) {
                                MiniPlayerContent(
                                    openPlayer = {
                                        targetExpandProgress = 1f
                                        scope.launch { sheetState.expand() }
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
        }
    }
}
