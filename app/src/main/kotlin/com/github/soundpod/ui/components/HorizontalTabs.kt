package com.github.soundpod.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HorizontalTabs(
    pagerState: PagerState,
    tabs: List<Int>,
    modifier: Modifier = Modifier,
    itemWidth: Dp = Dp.Unspecified,
    itemHeight: Dp = 60.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    minPadding: Dp = 6.dp
) {
    if (tabs.isEmpty()) return

    key(tabs) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val titleMediumStyle = MaterialTheme.typography.titleMedium
        val textMeasurer = rememberTextMeasurer()

        val tabTitles = tabs.map { stringResource(id = it) }

        val textWidthsPx = remember(tabTitles, titleMediumStyle) {
            tabTitles.map { text ->
                textMeasurer.measure(
                    text = text,
                    style = titleMediumStyle,
                    maxLines = 1
                ).size.width
            }
        }

        val lazyListState = rememberLazyListState()
        val snapFlingBehavior = rememberSnapFlingBehavior(
            lazyListState = lazyListState,
            snapPosition = ViewportCenterSnapPosition
        )

        val isTabsDragged by lazyListState.interactionSource.collectIsDraggedAsState()
        val isContentDragged by pagerState.interactionSource.collectIsDraggedAsState()
        var isTabGestureActive by remember { mutableStateOf(false) }

        LaunchedEffect(isTabsDragged) {
            if (isTabsDragged) {
                isTabGestureActive = true
            }
        }

        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(itemHeight),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = maxWidth
            val viewportWidthPx = constraints.maxWidth.coerceAtLeast(1)

            // Samsung Music-style compact per-tab spacing adapted to screen width,
            // while always keeping at least minPadding on each side.
            val adaptiveSidePadding = when {
                screenWidth < 360.dp -> minPadding + 2.dp
                screenWidth < 600.dp -> minPadding + 4.dp
                screenWidth < 840.dp -> minPadding + 8.dp
                else -> minPadding + 12.dp
            }

            val minPaddingPx = with(density) { minPadding.toPx() }
            val sidePaddingPx = with(density) { adaptiveSidePadding.toPx() }
            val explicitWidthPx = if (itemWidth != Dp.Unspecified && itemWidth > 0.dp) {
                with(density) { itemWidth.toPx() }
            } else {
                0f
            }

            val maxAllowedTabWidthPx = (viewportWidthPx - minPaddingPx * 2f).coerceAtLeast(minPaddingPx * 2f)

            val tabWidthsPx = remember(textWidthsPx, sidePaddingPx, explicitWidthPx, maxAllowedTabWidthPx) {
                textWidthsPx.map { textWidth ->
                    val rawWidth = if (explicitWidthPx > 0f) {
                        explicitWidthPx
                    } else {
                        textWidth + sidePaddingPx * 2f
                    }
                    rawWidth.coerceAtMost(maxAllowedTabWidthPx).roundToInt().coerceAtLeast(1)
                }
            }

            val prefixSumPx = remember(tabWidthsPx) {
                IntArray(tabWidthsPx.size + 1).also { sums ->
                    for (i in tabWidthsPx.indices) {
                        sums[i + 1] = sums[i] + tabWidthsPx[i]
                    }
                }
            }

            val tabCentersPx = remember(tabWidthsPx, prefixSumPx) {
                FloatArray(tabWidthsPx.size) { i ->
                    prefixSumPx[i] + tabWidthsPx[i] / 2f
                }
            }

            val startPadding = with(density) {
                ((viewportWidthPx - tabWidthsPx.first()) / 2).coerceAtLeast(0).toDp()
            }
            val endPadding = with(density) {
                ((viewportWidthPx - tabWidthsPx.last()) / 2).coerceAtLeast(0).toDp()
            }

            suspend fun scrollToFraction(position: Float) {
                val clamped = position.coerceIn(0f, (tabs.size - 1).toFloat())
                val lower = clamped.toInt().coerceIn(0, tabs.size - 1)
                val upper = (lower + 1).coerceIn(0, tabs.size - 1)
                val fraction = clamped - lower
                val targetCenter = tabCentersPx[lower] + (tabCentersPx[upper] - tabCentersPx[lower]) * fraction
                val totalScroll = (targetCenter - tabCentersPx[0]).coerceAtLeast(0f)

                var targetIndex = 0
                for (i in tabs.indices) {
                    if (totalScroll >= prefixSumPx[i]) {
                        targetIndex = i
                    } else {
                        break
                    }
                }
                val itemOffset = (totalScroll - prefixSumPx[targetIndex]).roundToInt().coerceAtLeast(0)
                lazyListState.scrollToItem(targetIndex, itemOffset)
            }

            // Keep tab strip synced with content pager unless the user is dragging/snapping the tab strip
            LaunchedEffect(tabs, pagerState, lazyListState, tabCentersPx) {
                snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
                    .collect { position ->
                        if (!isTabGestureActive) {
                            scrollToFraction(position)
                        }
                    }
            }

            val currentTabFraction by remember(lazyListState, prefixSumPx, tabCentersPx) {
                derivedStateOf {
                    val firstVisible = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()
                    if (firstVisible == null) {
                        pagerState.currentPage.toFloat()
                    } else {
                        val currentScroll = (prefixSumPx[firstVisible.index] - firstVisible.offset).toFloat()
                        val center = tabCentersPx[0] + currentScroll
                        when {
                            center <= tabCentersPx.first() -> 0f
                            center >= tabCentersPx.last() -> (tabs.size - 1).toFloat()
                            else -> {
                                var frac = 0f
                                for (i in 0 until tabs.size - 1) {
                                    if (center >= tabCentersPx[i] && center <= tabCentersPx[i + 1]) {
                                        val span = (tabCentersPx[i + 1] - tabCentersPx[i]).coerceAtLeast(1f)
                                        frac = i + (center - tabCentersPx[i]) / span
                                        break
                                    }
                                }
                                frac
                            }
                        }
                    }
                }
            }

            // When user finishes dragging/flinging the tab strip, snap content pager to the centered tab
            LaunchedEffect(isTabsDragged) {
                if (!isTabsDragged && isTabGestureActive) {
                    snapshotFlow { lazyListState.isScrollInProgress }
                        .filter { !it }
                        .collectLatest {
                            if (!isContentDragged && !pagerState.isScrollInProgress) {
                                val targetPage = currentTabFraction.roundToInt().coerceIn(0, tabs.size - 1)
                                try {
                                    if (targetPage != pagerState.currentPage) {
                                        pagerState.animateScrollToPage(targetPage)
                                    }
                                } finally {
                                    isTabGestureActive = false
                                    scrollToFraction(pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                }
                            } else {
                                isTabGestureActive = false
                            }
                        }
                }
            }

            val activeFractionalIndex by remember(pagerState, isTabGestureActive) {
                derivedStateOf {
                    if (isTabGestureActive) {
                        currentTabFraction
                    } else {
                        (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            .coerceIn(0f, (tabs.size - 1).toFloat())
                    }
                }
            }

            LazyRow(
                state = lazyListState,
                flingBehavior = snapFlingBehavior,
                contentPadding = PaddingValues(start = startPadding, end = endPadding),
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(
                    count = tabs.size,
                    key = { index -> tabs[index] }
                ) { index ->
                    val tabWidthDp = with(density) { tabWidthsPx[index].toDp() }
                    val transformation = calculateTabTransformation(
                        index = index,
                        currentFractionalIndex = activeFractionalIndex,
                        accentColor = accentColor,
                        secondaryColor = secondaryColor
                    )

                    Box(
                        modifier = Modifier
                            .width(tabWidthDp)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab
                            ) {
                                scope.launch {
                                    isTabGestureActive = false
                                    if (pagerState.currentPage == index) {
                                        scrollToFraction(index.toFloat())
                                    } else {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            }
                            .graphicsLayer {
                                scaleX = transformation.scale
                                scaleY = transformation.scale
                                alpha = transformation.alpha
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitles[index],
                            style = titleMediumStyle,
                            fontWeight = FontWeight.Normal,
                            color = transformation.color,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = minPadding)
                        )
                    }
                }
            }
        }
    }
}

private object ViewportCenterSnapPosition : SnapPosition {
    override fun position(
        layoutSize: Int,
        itemSize: Int,
        beforeContentPadding: Int,
        afterContentPadding: Int,
        itemIndex: Int,
        itemCount: Int
    ): Int = (layoutSize / 2) - (itemSize / 2) - beforeContentPadding
}

private data class TabTransformation(
    val scale: Float,
    val alpha: Float,
    val color: Color
)

private fun calculateTabTransformation(
    index: Int,
    currentFractionalIndex: Float,
    accentColor: Color,
    secondaryColor: Color
): TabTransformation {
    val pageOffset = currentFractionalIndex - index
    val fraction = (1f - abs(pageOffset)).coerceIn(0f, 1f)

    val scale = 0.8f + (0.4f * fraction)
    val color = lerp(
        start = secondaryColor.copy(alpha = 0.5f),
        stop = accentColor,
        fraction = fraction
    )
    val alpha = (((scale - 0.8f) / 0.4f) * 0.4f + 0.6f).coerceIn(0.6f, 1f)

    return TabTransformation(scale, alpha, color)
}
