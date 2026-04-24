package com.github.soundpod.ui.screens.player.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaMetadata
import com.github.core.ui.LocalAppearance
import com.github.soundpod.ui.components.LoadingAnimation
import com.github.soundpod.ui.components.Overlay
import com.github.soundpod.utils.LyricsData
import com.github.soundpod.viewmodels.LyricsViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun LyricsOverlay(
    modifier: Modifier = Modifier,
    mediaId: String?,
    mediaMetadata: MediaMetadata?,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    viewModel: LyricsViewModel = viewModel()
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val (colorPalette) = LocalAppearance.current
    val lyricsData by viewModel.lyricsData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(mediaId) {
        viewModel.loadLyrics(mediaId)
    }

    Overlay(
        modifier = modifier,
        lazyListState = lazyListState,
        enableScrollbar = false,
        headerContent = {
            Text(
                text = mediaMetadata?.title?.toString() ?: "Lyrics",
                style = MaterialTheme.typography.titleMedium,
                color = colorPalette.text,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp),
                maxLines = 1
            )
        }
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingAnimation(modifier = Modifier.size(50.dp))
            }
        } else {
            when (val currentLyrics = lyricsData) {
                is LyricsData.Synced -> {
                    val activeIndex by remember(currentPositionMs, currentLyrics.lines) {
                        derivedStateOf {
                            viewModel.getActiveIndex(currentPositionMs, currentLyrics.lines)
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val halfHeightDp = maxHeight / 2

                        LaunchedEffect(activeIndex) {
                            if (activeIndex >= 0) {
                                if (!lazyListState.isScrollInProgress) {
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(
                                            index = activeIndex,
                                            scrollOffset = 0
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = halfHeightDp),
                            verticalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            itemsIndexed(
                                items = currentLyrics.lines,
                                key = { _, line -> "${mediaId}_${line.startMs}" }
                            ) { index, line ->
                                val distance = abs(index - activeIndex)
                                val isActive = index == activeIndex

                                val textColor by animateColorAsState(
                                    targetValue = if (isActive) colorPalette.text else colorPalette.textDisabled,
                                    animationSpec = tween(400),
                                    label = "color"
                                )

                                val scale by animateFloatAsState(
                                    targetValue = if (isActive) 1.15f else 1.0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "scale"
                                )

                                val blurRadius by animateFloatAsState(
                                    targetValue = when {
                                        isActive -> 0f
                                        distance == 1 -> 1.5f
                                        distance == 2 -> 3f
                                        else -> 5f
                                    },
                                    animationSpec = tween(400),
                                    label = "blur"
                                )

                                val alpha by animateFloatAsState(
                                    targetValue = when {
                                        isActive -> 1f
                                        distance == 1 -> 0.7f
                                        distance == 2 -> 0.4f
                                        else -> 0.1f
                                    },
                                    animationSpec = tween(400),
                                    label = "alpha"
                                )

                                Text(
                                    text = line.text,
                                    color = textColor,
                                    fontSize = 26.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                    lineHeight = 36.sp,
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            transformOrigin = TransformOrigin(0f, 0.5f)
                                        }
                                        .alpha(alpha)
                                        .blur(blurRadius.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onSeekTo(line.startMs) }
                                )
                            }
                        }
                    }
                }

                is LyricsData.Unsynced -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        item {
                            Text(
                                text = currentLyrics.text,
                                color = colorPalette.text,
                                fontSize = 18.sp,
                                lineHeight = 32.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                LyricsData.None -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No lyrics found",
                            color = colorPalette.textDisabled,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}