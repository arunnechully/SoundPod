package com.github.soundpod.ui.screens.player

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.ActionInfo
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.MusicBars
import com.github.soundpod.ui.components.QueuedMediaItemMenu
import com.github.soundpod.ui.components.SwipeToActionBox
import com.github.soundpod.ui.items.ListItemPlaceholder
import com.github.soundpod.ui.items.MediaSongItem
import com.github.soundpod.ui.styling.onOverlay
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.queueLoopEnabledKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.shouldBePlaying
import com.github.soundpod.utils.windows
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("FrequentlyChangedStateReadInComposition")
@Composable
fun PlaylistOverlay(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val glassColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.07f)
    } else {
        Color.Black.copy(alpha = 0.04f)
    }
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val menuState = LocalMenuState.current

    var queueLoopEnabled by rememberPreference(queueLoopEnabledKey, defaultValue = false)
    var mediaItemIndex by remember {
        mutableIntStateOf(if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex)
    }
    var windows by remember { mutableStateOf(player.currentTimeline.windows) }
    var shouldBePlaying by remember { mutableStateOf(binder.player.shouldBePlaying) }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItemIndex =
                    if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                windows = timeline.windows
                mediaItemIndex =
                    if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = mediaItemIndex)
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        player.moveMediaItem(from.index, to.index)
    }

    val musicBarsTransition = updateTransition(targetState = mediaItemIndex, label = "bars")

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = stringResource(id = R.string.song_deleted_queue)
    val snackbarActionLabel = stringResource(id = R.string.undo)
    Box(
        modifier
            .clip(RoundedCornerShape(25.dp))
            .background(glassColor)
    ) {
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val showScrollToTop by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 3 || listState.firstVisibleItemScrollOffset > 0
            }
        }
        var showScrollbar by remember { mutableStateOf(false) }

        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                showScrollbar = true
            } else {
                delay(1500)
                showScrollbar = false
            }
        }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
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
//                        .background(colorPalette.textDisabled)
                        .clickable(onClick = {}), contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = "Shuffle",
                        tint = textColor,
                        modifier = Modifier.size(22.dp)

                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp),
                thickness = 0.5.dp,
                color = textColor.copy(alpha = 0.1f)
            )

            // Queue List
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1F)
            ) {
                items(
                    items = windows,
                    key = { it.uid.hashCode() }
                ) { window ->
                    val isPlayingThisMediaItem = mediaItemIndex == window.firstPeriodIndex
                    val currentWindow by rememberUpdatedState(window)

                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = window.uid.hashCode()
                    ) {
                        SwipeToActionBox(
                            destructiveAction = ActionInfo(
                                enabled = !isPlayingThisMediaItem,
                                onClick = {
                                    val deletedMediaItem = window.mediaItem
                                    val itemIndex = currentWindow.firstPeriodIndex

                                    player.removeMediaItem(itemIndex)

                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()

                                        val result = snackbarHostState.showSnackbar(
                                            message = snackbarMessage,
                                            actionLabel = snackbarActionLabel,
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Short
                                        )

                                        if (result == SnackbarResult.ActionPerformed) player.addMediaItem(
                                            itemIndex,
                                            deletedMediaItem
                                        )
                                    }
                                },
                                icon = Icons.Outlined.PlaylistRemove,
                                description = R.string.remove_from_queue
                            )
                        ) {
                            MediaSongItem(
                                song = window.mediaItem,
                                onClick = {
                                    if (isPlayingThisMediaItem) {
                                        if (shouldBePlaying) player.pause()
                                        else player.play()
                                    } else {
                                        player.seekToDefaultPosition(window.firstPeriodIndex)
                                        player.playWhenReady = true
                                    }
                                },
                                onLongClick = {
                                    menuState.display {
                                        QueuedMediaItemMenu(
                                            mediaItem = window.mediaItem,
                                            indexInQueue = if (isPlayingThisMediaItem) null else window.firstPeriodIndex,
                                            onDismiss = menuState::hide,
                                            onGoToAlbum = onGoToAlbum,
                                            onGoToArtist = onGoToArtist
                                        )
                                    }
                                },
                                onThumbnailContent = {
                                    musicBarsTransition.AnimatedVisibility(
                                        visible = { it == window.firstPeriodIndex },
                                        enter = fadeIn(tween(800)),
                                        exit = fadeOut(tween(800)),
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
                                                    modifier = Modifier
                                                        .height(24.dp)
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
                                        Icon(
                                            imageVector = Icons.Outlined.DragHandle,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    if (binder.isLoadingRadio) {
                        Column(
                            modifier = Modifier.shimmer()
                        ) {
                            repeat(3) { index ->
                                ListItemPlaceholder(
                                    modifier = Modifier.alpha(1f - index * 0.125f)
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(initialAlpha = 0.3f),
            exit = fadeOut(targetAlpha = 0f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
//                    .background(colorPalette.background2.copy(alpha = 0.9f))
                    .clickable {
                        coroutineScope.launch {
                            listState.animateScrollToItem(
                                index = 0,
                                scrollOffset = 0
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.chevron_up),
                    contentDescription = null,
//                    colorFilter = ColorFilter.tint(colorPalette.text),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Draggable Scrollbar
        val isScrollable = listState.layoutInfo.totalItemsCount > listState.layoutInfo.visibleItemsInfo.size
        AnimatedVisibility(
            visible = isScrollable && showScrollbar,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val thumbHeight = 70.dp
            val thumbWidth = 24.dp

            val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
            val totalContentHeight = listState.layoutInfo.totalItemsCount *
                    (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 50f)

            val scrollableHeight = (totalContentHeight - viewportHeight).coerceAtLeast(1f)
            val thumbTrackHeight = (viewportHeight - with(LocalDensity.current) { thumbHeight.toPx() }).coerceAtLeast(1f)

            val currentScrollOffset = listState.firstVisibleItemIndex *
                    (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 50f) +
                    listState.firstVisibleItemScrollOffset

            val thumbY = (currentScrollOffset / scrollableHeight) * thumbTrackHeight

            Box( // Track
                modifier = Modifier
                    .padding(end = 8.dp, top = 56.dp, bottom = 56.dp)
                    .width(thumbWidth)
                    .fillMaxHeight()
            ) {
                Box( // Thumb
                    modifier = Modifier
                        .offset(y = with(LocalDensity.current) { thumbY.toDp() })
                        .width(thumbWidth)
                        .height(thumbHeight)
                        .clip(RoundedCornerShape(12.dp))
//                        .background(colorPalette.textDisabled.copy(alpha = 0.7f))
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    val scrollDelta = dragAmount * (scrollableHeight / thumbTrackHeight)
                                    listState.scrollBy(scrollDelta)
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
//                            colorFilter = ColorFilter.tint(colorPalette.text.copy(alpha = 0.5f)),
                            modifier = Modifier.size(18.dp)
                        )
                        Image(
                            painter = painterResource(R.drawable.chevron_down),
                            contentDescription = "Scroll down cue",
//                            colorFilter = ColorFilter.tint(colorPalette.text.copy(alpha = 0.5f)),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}