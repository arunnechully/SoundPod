package com.github.soundpod.ui.screens.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AnimatedSeekbar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    color: Color = LocalAppearance.current.colorPalette.text,
    trackColor: Color = color.copy(alpha = 0.3f),
    isPlaying: Boolean = false,
    animationEnabled: Boolean = true
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(value) }
    var lastDragValue by remember { mutableFloatStateOf(value) }
    var isDraggingForward by remember { mutableStateOf(true) }

    // Update dragValue when value changes and not dragging
    LaunchedEffect(value) {
        if (!isDragging) {
            dragValue = value
            lastDragValue = value
        }
    }

    val shouldAnimate = isPlaying && !isDragging && animationEnabled

    val infiniteTransition = rememberInfiniteTransition(label = "seekbar_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(
        modifier = modifier
            .height(40.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        // Update initial drag value based on touch position
                        dragValue = ((offset.x / size.width) *
                            (valueRange.endInclusive - valueRange.start) +
                            valueRange.start).coerceIn(valueRange)
                        lastDragValue = dragValue
                        onValueChange(dragValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        dragValue = value // Reset to current value on cancel
                        onValueChangeFinished()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val range = valueRange.endInclusive - valueRange.start
                        val delta = (dragAmount / size.width) * range
                        val newDragValue = (dragValue + delta).coerceIn(valueRange)

                        // Only update if the change is significant enough
                        if (abs(newDragValue - dragValue) > (range * 0.001f)) {
                            isDraggingForward = newDragValue > lastDragValue
                            lastDragValue = dragValue
                            dragValue = newDragValue
                            onValueChange(dragValue)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newValue = ((offset.x / size.width) *
                        (valueRange.endInclusive - valueRange.start) +
                        valueRange.start).coerceIn(valueRange)
                    dragValue = newValue
                    onValueChange(newValue)
                    onValueChangeFinished()
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val amplitude = height * 0.2f
        val frequency = 2f

        // Calculate wave phase based on playing state and drag direction
        val currentPhase = when {
            isDragging -> 0f  // No animation while dragging
            shouldAnimate -> phase
            else -> 0f
        }

        // Draw background track
        val backgroundPath = Path()
        for (x in 0..width.toInt()) {
            val xRatio = x / width
            val y = if (shouldAnimate) {  // Update condition
                height / 2 + amplitude * sin(xRatio * frequency * 2 * PI.toFloat() + currentPhase)
            } else {
                height / 2  // Straight line when not animating
            }
            if (x == 0) {
                backgroundPath.moveTo(x.toFloat(), y)
            } else {
                backgroundPath.lineTo(x.toFloat(), y)
            }
        }

        drawPath(
            path = backgroundPath,
            color = trackColor,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw progress
        val progress = if (isDragging) {
            (dragValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        } else {
            (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        }

        val progressPath = Path()
        val progressWidth = width * progress
        for (x in 0..progressWidth.toInt()) {
            val xRatio = x / width
            val y = if (shouldAnimate) {  // Update condition
                height / 2 + amplitude * sin(xRatio * frequency * 2 * PI.toFloat() + currentPhase)
            } else {
                height / 2  // Straight line when not animating
            }
            if (x == 0) {
                progressPath.moveTo(x.toFloat(), y)
            } else {
                progressPath.lineTo(x.toFloat(), y)
            }
        }

        drawPath(
            path = progressPath,
            color = color,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw thumb
        val thumbX = width * progress
        val thumbY = if (shouldAnimate) {
            height / 2 + amplitude * sin(progress * frequency * 2 * PI.toFloat() + currentPhase)
        } else {
            height / 2  // Center position when not animating
        }
        drawCircle(
            color = color,
            radius = 8.dp.toPx(),
            center = Offset(thumbX, thumbY)
        )
    }
}
