package com.github.soundpod.ui.screens.player.seekbar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import kotlin.math.*

@Composable
fun PaperBoatAnimation(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    color: Color = LocalAppearance.current.colorPalette.text,
    trackColor: Color = color.copy(alpha = 0.2f),
    isPlaying: Boolean = false,
    animationEnabled: Boolean = true
) {
    val density = LocalDensity.current
    val boatSizePx = remember(density) { with(density) { 32.dp.toPx() } }
    val strokeWidthPx = remember(density) { with(density) { 1.dp.toPx() } }
    val gapPx = boatSizePx * 0f

    val progressPath = remember { Path() }
    val trackPath = remember { Path() }

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(value) }

    LaunchedEffect(value) {
        if (!isDragging) dragValue = value
    }

    val shouldAnimate = isPlaying && !isDragging && animationEnabled

    val infiniteTransition = rememberInfiniteTransition(label = "ocean")
    val mainPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "main_swell"
    )

    val currentAmp by animateFloatAsState(
        targetValue = if (shouldAnimate) 10.dp.value else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "amplitude"
    )

    val boatPainter = painterResource(id = R.drawable.paper_ship)

    Canvas(
        modifier = modifier
            .height(64.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val range = valueRange.endInclusive - valueRange.start
                        dragValue = (dragValue + (dragAmount / size.width) * range).coerceIn(valueRange)
                        onValueChange(dragValue)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newValue = ((offset.x / size.width) * (valueRange.endInclusive - valueRange.start) + valueRange.start).coerceIn(valueRange)
                    dragValue = newValue
                    onValueChange(newValue)
                    onValueChangeFinished()
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height * 0.5f

        val ampPx = currentAmp * density.density
        val mainFreq = 1.25f * (2 * PI).toFloat() / width
        val pMain = mainPhase

        val progress = (dragValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)
        val thumbX = width * progress

        progressPath.reset()
        val stopProgressX = (thumbX - gapPx).coerceAtLeast(0f)
        for (x in 0..stopProgressX.toInt() step 4) {
            val currX = x.toFloat()
            val y = centerY + ampPx * sin(currX * mainFreq + pMain)
            if (x == 0) progressPath.moveTo(currX, y) else progressPath.lineTo(currX, y)
        }
        drawPath(progressPath, color, style = Stroke(strokeWidthPx, cap = StrokeCap.Round))

        trackPath.reset()
        val startTrackX = (thumbX + gapPx).coerceAtMost(width)
        for (x in startTrackX.toInt()..width.toInt() step 4) {
            val currX = x.toFloat()
            val y = centerY + ampPx * sin(currX * mainFreq + pMain)
            if (x == startTrackX.toInt()) trackPath.moveTo(currX, y) else trackPath.lineTo(currX, y)
        }
        drawPath(trackPath, trackColor, style = Stroke(strokeWidthPx, cap = StrokeCap.Round))

        val thumbY = centerY + ampPx * sin(thumbX * mainFreq + pMain)
        val slope = ampPx * mainFreq * cos(thumbX * mainFreq + pMain)
        val angleDegrees = Math.toDegrees(atan(slope.toDouble())).toFloat()

        withTransform({
            translate(thumbX, thumbY)
            rotate(angleDegrees, pivot = Offset.Zero)
            translate(left = -boatSizePx / 2f, top = -boatSizePx * 0.77f)
        }) {
            with(boatPainter) {
                draw(size = Size(boatSizePx, boatSizePx), colorFilter = ColorFilter.tint(color))
            }
        }
    }
}