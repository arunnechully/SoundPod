package com.github.soundpod.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.navigation.NavController
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.ui.appearance.PlayerBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScaffold(
    navController: NavController,
    sheetState: SheetState,
    scaffoldPadding: PaddingValues,
    showPlayer: Boolean,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current

    var targetExpandProgress by remember { mutableStateOf(0f) }

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val exactScreenHeight = maxHeight
        val exactScreenWidth = maxWidth

        val systemBottomPadding = scaffoldPadding.calculateBottomPadding()
        val activeBottomPadding = lerp(systemBottomPadding, 0.dp, expandProgress)
        val playerHeight = lerp(65.dp, exactScreenHeight, expandProgress)
        val topCornerRadius = lerp(32.dp, 0.dp, expandProgress)

        CompositionLocalProvider(value = LocalPlayerPadding provides (65.dp + systemBottomPadding)) {
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = activeBottomPadding)
                    .width(exactScreenWidth)
                    .height(playerHeight)
                    .clip(RoundedCornerShape(topStart = topCornerRadius, topEnd = topCornerRadius))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val sensitivity = 0.0015f
                                targetExpandProgress =
                                    (targetExpandProgress - dragAmount * sensitivity).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                targetExpandProgress = if (targetExpandProgress > 0.4f) 1f else 0f
                                scope.launch {
                                    if (targetExpandProgress == 1f) sheetState.expand() else sheetState.partialExpand()
                                }
                            }
                        )
                    }
            ) {
                PlayerBackground(thumbnailUrl = currentArtworkUrl) {

                    if (expandProgress > 0f) {

                        val containerTopY = exactScreenHeight - activeBottomPadding - playerHeight

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .requiredHeight(exactScreenHeight)
                                .graphicsLayer {
                                    translationY = -containerTopY.toPx()
                                    alpha = expandProgress
                                }
                        ) {
                            MainPlayerContent(
                                onGoToAlbum = {},
                                onGoToArtist = {},
                                onBack = {
                                    targetExpandProgress = 0f
                                    scope.launch { sheetState.partialExpand() }
                                }
                            )
                        }
                    }

                    if (expandProgress < 1f) {
                        val miniPlayerTranslateY = systemBottomPadding - activeBottomPadding

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(65.dp)
                                .graphicsLayer {
                                    translationY = miniPlayerTranslateY.toPx()
                                    alpha = (1f - (expandProgress * 3.33f)).coerceIn(0f, 1f)
                                }
                        ) {
                            MiniPlayerContent(
                                openPlayer = {
                                    targetExpandProgress = 1f
                                    scope.launch { sheetState.expand() }
                                }
                            )
                        }
                    }
                    SharedThumbnail(expandProgress = expandProgress)
                }
            }
        }
    }
}