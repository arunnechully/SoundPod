package com.github.soundpod.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.NavController
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.ui.appearance.PlayerBackground
import com.github.soundpod.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedPlayer(
    navController: NavController,
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
        animationSpec = spring(stiffness = 400f),
        label = "expandAnimation"
    )

    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player
    var currentArtworkUrl by remember {
        mutableStateOf(player?.currentMediaItem?.mediaMetadata?.artworkUri?.toString())
    }

    LaunchedEffect(expandProgress) {
        if (expandProgress == 0f) {
            showLyrics = false
            showPlaylist = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val exactScreenHeight = maxHeight
        val exactScreenWidth = maxWidth

        val systemBottomPadding = scaffoldPadding.calculateBottomPadding()
        val activeBottomPadding = lerp(systemBottomPadding, 0.dp, expandProgress)
        val playerHeight = lerp(60.dp, exactScreenHeight, expandProgress)

        val cornerRadius = lerp(28.dp, 0.dp, expandProgress)

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
            val dragGestureModifier = if (showPlaylist || showLyrics) {
                Modifier
            } else {
                Modifier.pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val sensitivity = 0.0015f
                            targetExpandProgress = (targetExpandProgress - dragAmount * sensitivity).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            targetExpandProgress = if (targetExpandProgress > 0.4f) 1f else 0f
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
                                    .fillMaxSize()
                                    .alpha(expandProgress)
                            ) {
                                PlayerLayout(
                                    onGoToAlbum = { browseId ->
                                        scope.launch { sheetState.partialExpand() }
                                        navController.navigate(route = Routes.Album(id = browseId))
                                    },
                                    onGoToArtist = { browseId ->
                                        scope.launch { sheetState.partialExpand() }
                                        navController.navigate(route = Routes.Artist(id = browseId))
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