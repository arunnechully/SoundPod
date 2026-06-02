package com.github.soundpod.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
    itemWidth: Dp = 80.dp,
    itemHeight: Dp = 60.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val scope = rememberCoroutineScope()

    val tabPagerState = rememberPagerState(
        initialPage = pagerState.currentPage,
        pageCount = { tabs.size }
    )

    val isTabsDragged by tabPagerState.interactionSource.collectIsDraggedAsState()
    val isContentDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var isContentCatchingUp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            .collect { position ->
                if (!isTabsDragged && !isContentCatchingUp) {
                    val page = position.roundToInt().coerceIn(0, tabs.size - 1)
                    val offset = (position - page).coerceIn(-0.5f, 0.5f)
                    tabPagerState.scrollToPage(page, offset)
                }
            }
    }

    LaunchedEffect(isTabsDragged) {
        if (!isTabsDragged) {
            snapshotFlow { tabPagerState.isScrollInProgress }
                .filter { !it }
                .collectLatest {
                    if (!isContentDragged && !pagerState.isScrollInProgress) {
                        val targetPage = tabPagerState.currentPage
                        if (targetPage != pagerState.currentPage) {
                            isContentCatchingUp = true
                            pagerState.animateScrollToPage(targetPage)
                            isContentCatchingUp = false
                        }
                    }
                }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight),
        contentAlignment = Alignment.Center
    ) {
        val centerPadding = (maxWidth - itemWidth) / 2

        HorizontalPager(
            state = tabPagerState,
            pageSize = PageSize.Fixed(itemWidth),
            contentPadding = PaddingValues(horizontal = centerPadding),
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            beyondViewportPageCount = 4
        ) { index ->
            val transformation = calculateTabTransformation(
                index = index,
                pagerState = tabPagerState,
                accentColor = accentColor,
                secondaryColor = secondaryColor
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab
                    ) {
                        scope.launch {
                            isContentCatchingUp = true
                            try {
                                launch { tabPagerState.animateScrollToPage(index) }
                                pagerState.animateScrollToPage(index)
                            } finally { isContentCatchingUp = false }
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
                    text = stringResource(id = tabs[index]),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = transformation.color
                )
            }
        }
    }
}

private data class TabTransformation(
    val scale: Float,
    val alpha: Float,
    val color: Color
)

private fun calculateTabTransformation(
    index: Int,
    pagerState: PagerState,
    accentColor: Color,
    secondaryColor: Color
): TabTransformation {
    val pageOffset = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction
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
