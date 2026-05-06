package com.github.soundpod.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(25.dp),
    content: @Composable ColumnScope.() -> Unit
) {

    val (colorPalette) = LocalAppearance.current

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = shape,
        color = colorPalette.boxColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
