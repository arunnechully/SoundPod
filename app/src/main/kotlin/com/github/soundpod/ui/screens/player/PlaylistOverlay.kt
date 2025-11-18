package com.github.soundpod.ui.screens.player

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.MusicBars
import com.github.soundpod.ui.components.QueuedMediaItemMenu
import com.github.soundpod.ui.components.SwipeToActionBox
import com.github.soundpod.ui.items.MediaSongItem
import com.github.soundpod.ui.styling.onOverlay
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.shouldBePlaying
import com.github.soundpod.utils.windows
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("FrequentlyChangedStateReadInComposition")
@Composable
fun PlaylistOverlay(
    modifier: Modifier = Modifier,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    val (colorPalette, _) = LocalAppearance.current
    val isDarkTheme = colorPalette.background2.luminance() < 0.5f
    val glassColor = if (isDarkTheme) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f)

    // ----- state derived from player/timeline -----
    var windows by remember { mutableStateOf(player.currentTimeline.windows) }
    var mediaItemIndex by remember { mutableIntStateOf(if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex) }
    var shouldBePlaying by remember { mutableStateOf(player.shouldBePlaying) }

    // Listen to player changes, minimal updates
    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItemIndex = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                windows = timeline.windows
                mediaItemIndex = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = player.shouldBePlaying
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = player.shouldBePlaying
            }
        }
    }

    // ----- single LazyListState used by reorderable -----
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = mediaItemIndex)
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // perform move on player (background thread in your player impl)
        player.moveMediaItem(from.index, to.index)
    }

    // ----- scrollbar + "scroll to top" - driven by snapshotFlow to avoid heavy recomposition -----
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 3 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    var showScrollbar by remember { mutableStateOf(false) }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            showScrollbar = true
        } else {
            // keep visible for a short time after scroll stops
            delay(700)
            showScrollbar = false
        }
    }

    // Small helper to avoid re-evaluating callbacks inside items
    val onGoToAlbumState = rememberUpdatedState(onGoToAlbum)
    val onGoToArtistState = rememberUpdatedState(onGoToArtist)

    // animation helper for thumbnail music bars
    val musicBarsTransition = androidx.compose.animation.core.updateTransition(targetState = mediaItemIndex, label = "bars")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(25.dp))
            .background(glassColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            // header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(shape = CircleShape)
                        .background(colorPalette.textDisabled)
                        .clickable(onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = "add",
                        tint = colorPalette.text,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // (you can add actions here)
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = colorPalette.text.copy(alpha = 0.1f)
            )

            // ----- the actual list -----
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Top,
                userScrollEnabled = true
            ) {
                itemsIndexed(
                    items = windows,
                    key = { _, window -> window.uid.hashCode() }
                ) { index, window ->
                    // keep a stable reference to the window for this composition
                    val currentWindow = remember(window) { window }
                    val isPlayingThisMediaItem = remember(mediaItemIndex, currentWindow.firstPeriodIndex) {
                        mediaItemIndex == currentWindow.firstPeriodIndex
                    }
                    ReorderableItem(
                        state = reorderableState,
                        key = currentWindow.uid.hashCode()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .smoothReorderAnimation()
                        ) {
                            SwipeToActionBox(
                                destructiveAction = com.github.soundpod.models.ActionInfo(
                                    enabled = !isPlayingThisMediaItem,
                                    onClick = {
                                        val itemIndex = currentWindow.firstPeriodIndex
                                        player.removeMediaItem(itemIndex)
                                    },
                                    icon = Icons.Outlined.PlaylistRemove,
                                    description = R.string.remove_from_queue
                                )
                            ) {
                                MediaSongItem(
                                    modifier = Modifier,
                                    song = currentWindow.mediaItem,
                                    onClick = {
                                        if (isPlayingThisMediaItem) {
                                            if (shouldBePlaying) player.pause()
                                            else player.play()
                                        } else {
                                            player.seekToDefaultPosition(currentWindow.firstPeriodIndex)
                                            player.playWhenReady = true
                                        }
                                    },
                                    onLongClick = {
                                        menuState.display {
                                            QueuedMediaItemMenu(
                                                mediaItem = currentWindow.mediaItem,
                                                indexInQueue = if (isPlayingThisMediaItem) null else currentWindow.firstPeriodIndex,
                                                onDismiss = menuState::hide,
                                                onGoToAlbum = onGoToAlbumState.value,
                                                onGoToArtist = onGoToArtistState.value
                                            )
                                        }
                                    },
                                    onThumbnailContent = {
                                        musicBarsTransition.AnimatedVisibility(
                                            visible = { it == currentWindow.firstPeriodIndex },
                                            enter = fadeIn(tween(320)),
                                            exit = fadeOut(tween(200)),
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        color = Color.Black.copy(alpha = 0.25F),
                                                        shape = MaterialTheme.shapes.medium
                                                    )
                                            ) {
                                                if (shouldBePlaying) {
                                                    MusicBars(
                                                        color = MaterialTheme.colorScheme.onOverlay,
                                                        modifier = Modifier.height(24.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Filled.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = MaterialTheme.colorScheme.onOverlay
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier.draggableHandle()
                                        ) {
                                            Icon(imageVector = Icons.Outlined.DragHandle, contentDescription = null)
                                        }
                                    }
                                )
                            }
                            if (index < windows.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 0.5.dp,
                                    color = colorPalette.text.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(initialAlpha = 0.35f),
            exit = fadeOut(targetAlpha = 0f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorPalette.background2.copy(alpha = 0.95f))
                    .clickable {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.chevron_up),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colorPalette.text),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        val isScrollable = lazyListState.layoutInfo.totalItemsCount > lazyListState.layoutInfo.visibleItemsInfo.size
        AnimatedVisibility(
            visible = isScrollable && showScrollbar,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val thumbHeight = 70.dp
            val thumbWidth = 24.dp
            val viewportHeight = lazyListState.layoutInfo.viewportSize.height.toFloat()
            val itemSizePx = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 56f
            val totalContentHeight = lazyListState.layoutInfo.totalItemsCount * itemSizePx
            val scrollableHeight = (totalContentHeight - viewportHeight).coerceAtLeast(1f)
            val thumbTrackHeight = (viewportHeight - with(LocalDensity.current) { thumbHeight.toPx() }).coerceAtLeast(1f)
            val currentScrollOffset = lazyListState.firstVisibleItemIndex * itemSizePx + lazyListState.firstVisibleItemScrollOffset
            val thumbY = (currentScrollOffset / scrollableHeight) * thumbTrackHeight

            Box(
                modifier = Modifier
                    .padding(end = 8.dp, top = 56.dp, bottom = 56.dp)
                    .width(thumbWidth)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .offset(y = with(LocalDensity.current) { thumbY.toDp() })
                        .width(thumbWidth)
                        .height(thumbHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorPalette.textDisabled.copy(alpha = 0.72f))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val scrollDelta = dragAmount * (scrollableHeight / thumbTrackHeight)
                                    lazyListState.scrollBy(scrollDelta)
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(R.drawable.chevron_up),
                            contentDescription = "Scroll up cue",
                            colorFilter = ColorFilter.tint(colorPalette.text.copy(alpha = 0.5f)),
                            modifier = Modifier.size(18.dp)
                        )
                        Image(
                            painter = painterResource(R.drawable.chevron_down),
                            contentDescription = "Scroll down cue",
                            colorFilter = ColorFilter.tint(colorPalette.text.copy(alpha = 0.5f)),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.smoothReorderAnimation(): Modifier {
    val animatedOffset by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 200f
        )
    )
    return this.offset(y = animatedOffset.dp)
}

