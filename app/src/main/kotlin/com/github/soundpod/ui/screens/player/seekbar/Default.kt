package com.github.soundpod.ui.screens.player.seekbar

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

@Composable
fun SeekBar(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 3.dp,
    scrubberColor: Color = color,
    scrubberRadius: Dp = 16.dp,
    shape: Shape = RectangleShape,
) {
    val isDragging = remember { MutableTransitionState(false) }

    val transition = rememberTransition(transitionState = isDragging, label = "SeekbarTransition")

    val currentBarHeight by transition.animateDp(label = "BarHeight") { if (it) scrubberRadius else barHeight }
    val currentScrubberRadius by transition.animateDp(label = "ScrubberRadius") { if (it) 0.dp else 6.dp }

    val safeRange = (maximumValue - minimumValue).coerceAtLeast(1L)
    val progress = ((value - minimumValue).toFloat() / safeRange.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(32.dp)
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue <= minimumValue) return@pointerInput
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging.targetState = true
                        val startValue = (offset.x / size.width * safeRange + minimumValue).roundToLong()
                        onDragStart(startValue.coerceIn(minimumValue, maximumValue))
                    },
                    onHorizontalDrag = { _, delta ->
                        acc += (delta / size.width) * safeRange
                        if (acc !in -1f..1f) {
                            onDrag(acc.toLong())
                            acc -= acc.toLong()
                        }
                    },
                    onDragEnd = {
                        isDragging.targetState = false
                        acc = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging.targetState = false
                        acc = 0f
                        onDragEnd()
                    }
                )
            }
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue <= minimumValue) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        val tapValue = (offset.x / size.width * safeRange + minimumValue).roundToLong()
                        onDragStart(tapValue.coerceIn(minimumValue, maximumValue))
                        val released = tryAwaitRelease()
                        if (released) onDragEnd()
                    }
                )
            }
            .drawWithContent {
                drawContent()
                if (currentScrubberRadius > 0.dp) {
                    drawCircle(
                        color = scrubberColor,
                        radius = currentScrubberRadius.toPx(),
                        center = center.copy(x = progress * size.width)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(currentBarHeight)
                .fillMaxWidth()
                .clip(shape)
                .background(color = backgroundColor, shape = shape)
        ) {
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(color = color)
                )
            }
        }
    }
}