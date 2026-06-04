package com.github.soundpod.ui.appearance

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap

data class ColorClusters(
    val c1: Color,
    val c2: Color,
    val c3: Color,
    val surface: Color
)

suspend fun extractColorClusters(
    context: Context,
    thumbnailUrl: String?,
    fallbackColor: Color
): ColorClusters {
    if (thumbnailUrl.isNullOrEmpty()) {
        return ColorClusters(fallbackColor, fallbackColor, fallbackColor, fallbackColor)
    }

    val request = ImageRequest.Builder(context)
        .data(thumbnailUrl)
        .allowHardware(false)
        .build()

    val result = try {
        context.imageLoader.execute(request)
    } catch (_: Exception) {
        null
    }
    
    val bitmap = result?.image?.toBitmap()
    
    if (bitmap != null) {
        val palette = Palette.from(bitmap).generate()
        
        val vibrant = Color(palette.getVibrantColor(fallbackColor.toArgb()))
        val muted = Color(palette.getMutedColor(fallbackColor.toArgb()))
        val dominant = Color(palette.getDominantColor(fallbackColor.toArgb()))
        
        // Pick the best "surface" color based on swatches
        val surfaceColor = palette.vibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: fallbackColor.toArgb()
        
        return ColorClusters(vibrant, muted, dominant, Color(surfaceColor))
    }

    return ColorClusters(fallbackColor, fallbackColor, fallbackColor, fallbackColor)
}

fun Color.adaptToTheme(isDark: Boolean): Color {
    val lum = luminance()
    return if (isDark) {
        // If it's too dark in Dark Mode, lighten it
        if (lum < 0.15f) {
            this.copy(alpha = 1f).compositeOver(Color.White.copy(alpha = 0.2f))
        } else if (lum > 0.6f) {
            // If it's too bright in Dark Mode, tone it down
            this.copy(alpha = 1f).compositeOver(Color.Black).copy(alpha = 0.6f).compositeOver(Color.Black)
        } else this
    } else {
        // If it's too bright in Light Mode, darken it
        if (lum > 0.85f) {
            this.copy(alpha = 1f).compositeOver(Color.Black.copy(alpha = 0.15f))
        } else if (lum < 0.3f) {
            // If it's too dark in Light Mode, brighten it
            this.copy(alpha = 1f).compositeOver(Color.White).copy(alpha = 0.7f).compositeOver(Color.White)
        } else this
    }
}
