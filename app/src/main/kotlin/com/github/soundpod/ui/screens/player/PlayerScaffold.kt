package com.github.soundpod.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.ui.appearance.PlayerBackground
import com.github.soundpod.ui.navigation.Routes
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
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val (colorPalette) = LocalAppearance.current

    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player

    var currentArtworkUrl by remember {
        mutableStateOf(player?.currentMediaItem?.mediaMetadata?.artworkUri?.toString())
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentArtworkUrl = mediaItem?.mediaMetadata?.artworkUri?.toString()
            }
            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                currentArtworkUrl = mediaMetadata.artworkUri?.toString()
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    val targetPeekHeight = if (showPlayer) {
        40.dp + 20.dp + scaffoldPadding.calculateBottomPadding()
    } else {
        0.dp
    }

    val animatedPeekHeight by animateDpAsState(
        targetValue = targetPeekHeight,
        label = "peekHeight"
    )

    Box(
        modifier = Modifier
            .windowInsetsPadding(
                WindowInsets(
                    left = scaffoldPadding.calculateLeftPadding(layoutDirection),
                    right = scaffoldPadding.calculateRightPadding(layoutDirection)
                )
            )
    ) {
        BottomSheetScaffold(
            sheetShape = MaterialTheme.shapes.extraLarge,
            sheetContainerColor = MaterialTheme.colorScheme.background,
            sheetContent = {

                // 4. Use the Smart Background Wrapper
                PlayerBackground(thumbnailUrl = currentArtworkUrl) {

                    AnimatedContent(
                        targetState = sheetState.targetValue,
                        label = "player",
                        contentKey = { value ->
                            if (value == SheetValue.Expanded) 0 else 1
                        }
                    ) { value ->
                        Box(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            // Note: removed the hardcoded ThemedLottieBackground here
                            // because it's now handled inside PlayerBackground based on settings.

                            if (value == SheetValue.Expanded) {
                                NewPlayer(
                                    onGoToAlbum = { browseId ->
                                        scope.launch { sheetState.partialExpand() }
                                        navController.navigate(
                                            route = Routes.Album(id = browseId)
                                        )
                                    },
                                    onGoToArtist = { browseId ->
                                        scope.launch { sheetState.partialExpand() }
                                        navController.navigate(
                                            route = Routes.Artist(id = browseId)
                                        )
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    NewMiniPlayer(
                                        openPlayer = {
                                            scope.launch { sheetState.expand() }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            scaffoldState = scaffoldState,
            sheetPeekHeight = animatedPeekHeight,
            sheetMaxWidth = Int.MAX_VALUE.dp,
            sheetDragHandle = null
        ) {
            val targetPadding = if (showPlayer && sheetState.currentValue != SheetValue.Hidden) {
                scaffoldPadding.calculateBottomPadding() + 76.dp + 16.dp
            } else {
                scaffoldPadding.calculateBottomPadding()
            }

            val bottomPadding = animateDpAsState(
                targetValue = targetPadding,
                label = "padding"
            )

            CompositionLocalProvider(value = LocalPlayerPadding provides bottomPadding.value) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    content = content
                )
            }
        }
    }
}