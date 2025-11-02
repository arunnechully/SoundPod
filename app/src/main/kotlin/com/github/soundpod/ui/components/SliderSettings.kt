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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Define the colors from your image ---
val SalmonColor = Color(0xFFF08A6E)
val DarkGrayColor = Color(0xFF3D3D3D)
val AlmostBlackColor = Color(0xFF000000) // For the thumb center
val LightTextColor = Color(0xFFE0E0E0)
val ValueTextColor = Color(0xFFF08A6E) // The value text is also salmon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomStyledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String
) {
    // --- 1. Interaction State for animations ---
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsDraggedAsState()

    // --- 2. Animate values based on drag state ---
    val thumbAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0f else 1f,
        label = "ThumbAlpha"
    )
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 28.dp else 10.dp, // Expands from 10dp to 16dp
        label = "TrackHeight"
    )
    // ---------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // --- Labels ---
        Text(
            text = label,
            color = LightTextColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = valueLabel(value),
            color = ValueTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(4.dp))

        // --- Custom Slider ---
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            interactionSource = interactionSource, // --- 3. Pass interactionSource ---
            modifier = Modifier.fillMaxWidth(),

            // --- Custom Thumb ---
            thumb = {
                Box(
                    modifier = Modifier
                        .graphicsLayer { alpha = thumbAlpha } // --- 4. Apply animated alpha ---
                        .size(24.dp)
                        .background(AlmostBlackColor, CircleShape)
                        .border(3.dp, SalmonColor, CircleShape)
                )
            },

            // --- Custom Track ---
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
                        .background(DarkGrayColor, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(SalmonColor, CircleShape)
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
