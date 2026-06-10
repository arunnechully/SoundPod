package com.github.soundpod.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.core.ui.LocalAppearance

@Composable
fun CustomDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: IntOffset = IntOffset(x = -12, y = -90),
    endPadding: Dp = 15.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = expanded

    if (transitionState.currentState || transitionState.targetState) {
        val transition =
            rememberTransition(transitionState = transitionState, label = "PopupTransition")

        val scale by transition.animateFloat(
            transitionSpec = { tween(300, easing = LinearOutSlowInEasing) },
            label = "PopupScale"
        ) { if (it) 1f else 0.8f }

        val alpha by transition.animateFloat(
            transitionSpec = { tween(300, easing = LinearOutSlowInEasing) },
            label = "PopupAlpha"
        ) { if (it) 1f else 0f }
        val elevation by transition.animateDp(
            transitionSpec = { tween(300, easing = LinearOutSlowInEasing) },
            label = "PopupElevation"
        ) { if (it) 8.dp else 0.dp }

        Popup(
            alignment = Alignment.TopEnd,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, end = endPadding)
            ) {
                Surface(
                    modifier = modifier
                        .width(IntrinsicSize.Max)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(1f, 0f)
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                            shadowElevation = elevation.toPx()
                            shape = RoundedCornerShape(18.dp)
                            clip = false
                        },
                    color = colorPalette.boxColor,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier,
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = LocalAppearance.current.colorPalette.text
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = textColor
    )
}