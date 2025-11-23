package com.github.soundpod.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.core.ui.LocalAppearance
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderSettingsItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    hapticStep: Float = 0.05f,
    hapticUseIntegerStep: Boolean = false,
    hapticUseFloatStep: Boolean = false,
    hapticFloatStep: Float = 0.1f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()
    var lastValue by remember { mutableFloatStateOf(value) }
    val haptic = LocalHapticFeedback.current

    val thumbAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0f else 1f,
        label = "ThumbAlpha"
    )
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 28.dp else 10.dp,
        label = "TrackHeight"
    )

    val (colorPalette) = LocalAppearance.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = colorPalette.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = valueLabel(value),
            color = colorPalette.accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value,
            onValueChange = { newValue ->

                if (hapticUseIntegerStep) {
                    // 1 tick per whole number
                    val currentInt = newValue.toInt()
                    if (currentInt != lastValue.toInt()) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastValue = newValue
                    }

                } else if (hapticUseFloatStep) {

                    val diff = abs(newValue - lastValue)
                    if (diff >= hapticFloatStep) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastValue = newValue
                    }

                } else {
                    // percent-based fallback
                    val fullRange = valueRange.endInclusive - valueRange.start
                    val percentMoved = abs(newValue - lastValue) / fullRange

                    if (percentMoved >= hapticStep) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastValue = newValue
                    }
                }

                onValueChange(newValue)
            },
            valueRange = valueRange,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),

            thumb = {
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = thumbAlpha }
                        .size(24.dp)
                        .background(colorPalette.background3, CircleShape)
                        .border(3.dp, colorPalette.accent, CircleShape)
                )
            },

            track = { sliderState ->
                val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
                val fraction = if (range == 0f) {
                    0f
                } else {
                    ((sliderState.value - sliderState.valueRange.start) / range).coerceIn(0f, 1f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .background(colorPalette.onAccent, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(colorPalette.accent, CircleShape)
                    )
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}
