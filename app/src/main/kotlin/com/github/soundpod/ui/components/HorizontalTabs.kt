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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HorizontalTabs(
    pagerState: PagerState,
    tabs: List<Int>,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    val scope = rememberCoroutineScope()

    val tabPagerState = rememberPagerState(
        initialPage = pagerState.currentPage,
        pageCount = { tabs.size }
    )

    val isTabsDragged by tabPagerState.interactionSource.collectIsDraggedAsState()
    val isContentDragged by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage + pagerState.currentPageOffsetFraction }
            .collect { position ->
                if (!isTabsDragged) {
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
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                }
        }
    }

    val itemWidth = 80.dp
    val itemHeight = 60.dp

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
            val titleRes = tabs[index]
            val transformation by remember(index) {
                derivedStateOf {
                    calculateTabTransformation(
                        index = index,
                        pagerState = tabPagerState,
                        accentColor = colorPalette.accent,
                        secondaryColor = colorPalette.textSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        scope.launch {
                            launch { tabPagerState.animateScrollToPage(index) }
                            launch { pagerState.animateScrollToPage(index) }
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
                    text = stringResource(id = titleRes),
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
