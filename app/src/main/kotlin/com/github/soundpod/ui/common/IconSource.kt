package com.github.soundpod.ui.common

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter

sealed class IconSource {
    data class Vector(val imageVector: ImageVector) : IconSource()
    data class Icon(val painter: Painter) : IconSource()
}
