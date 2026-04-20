package com.github.soundpod.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Overlay(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    enableScrollToTop: Boolean = true,
    enableScrollbar: Boolean = true,
    headerContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val (colors) = LocalAppearance.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val showScrollToTop by remember(enableScrollToTop) {
        derivedStateOf { enableScrollToTop && lazyListState.firstVisibleItemIndex > 0 }
    }

    var showScrollbarState by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    val isScrolling = lazyListState.isScrollInProgress

    // Keep scrollbar visible if either the list is scrolling OR the user is dragging the thumb
    if (enableScrollbar) {
        LaunchedEffect(isScrolling, isDragging) {
            if (isScrolling || isDragging) {
                showScrollbarState = true
            } else {
                delay(1200)
                showScrollbarState = false
            }
        }
    }

    val scrollbarMetrics by remember(lazyListState, enableScrollbar) {
        derivedStateOf {
            if (!enableScrollbar) return@derivedStateOf null

            val layout = lazyListState.layoutInfo
            val first = layout.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf null

            val viewport = layout.viewportSize.height.toFloat()
            val itemSize = first.size.toFloat()
            val totalItems = layout.totalItemsCount

            if (totalItems == 0) return@derivedStateOf null

            val totalHeight = totalItems * itemSize
            if (totalHeight <= viewport) return@derivedStateOf null

            val scrollOffset =
                lazyListState.firstVisibleItemIndex * itemSize +
                        lazyListState.firstVisibleItemScrollOffset

            val scrollable = totalHeight - viewport

            val thumbHeight = with(density) { 64.dp.toPx() }
            val trackHeight = viewport - with(density) { 112.dp.toPx() }

            val range = (trackHeight - thumbHeight).coerceAtLeast(1f)
            val ratio = (scrollOffset / scrollable).coerceIn(0f, 1f)

            ScrollbarMetrics(
                thumbOffset = ratio * range,
                scrollFactor = scrollable / range
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(colors.glass)
    ) {
        Column(Modifier.fillMaxSize()) {
            headerContent()
            content()
        }

        if (enableScrollToTop) {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.glass)
                        .clickable {
                            scope.launch { lazyListState.animateScrollToItem(0) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.arrow_up),
                        contentDescription = "Scroll to top",
                        colorFilter = ColorFilter.tint(colors.text),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (enableScrollbar) {
            scrollbarMetrics?.let { metrics ->
                // Capture the latest scrollFactor without restarting the pointerInput
                val currentScrollFactor by rememberUpdatedState(metrics.scrollFactor)

                AnimatedVisibility(
                    visible = showScrollbarState,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 56.dp, horizontal = 4.dp)
                            .width(28.dp)
                            .fillMaxHeight()
                            // Pass Unit to pointerInput so it NEVER restarts during a drag
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = { isDragging = false },
                                    onDragCancel = { isDragging = false }
                                ) { change, dragAmount ->
                                    change.consume()
                                    // Use dispatchRawDelta for synchronous, glitch-free scrolling
                                    lazyListState.dispatchRawDelta(dragAmount * currentScrollFactor)
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(20.dp)
                                .height(64.dp)
                                .graphicsLayer {
                                    translationY = metrics.thumbOffset
                                }
                                .background(
                                    color = colors.glass,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            ScrollArrows(colors.text)
                        }
                    }
                }
            }
        }
    }
}

private data class ScrollbarMetrics(
    val thumbOffset: Float,
    val scrollFactor: Float
)

@Composable
private fun ScrollArrows(color: Color) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_up),
            contentDescription = null,
            tint = color.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp)
        )
        Icon(
            painter = painterResource(R.drawable.arrow_down),
            contentDescription = null,
            tint = color.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
fun CircleDragHandle(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        ScrollArrows(tint)
    }
}