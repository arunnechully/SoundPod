package com.github.soundpod.ui.appearance

import android.content.Context
import androidx.compose.ui.graphics.Color
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
    val c4: Color,
    val c5: Color,
    val surface: Color
)

suspend fun extractColorClusters(
    context: Context,
    thumbnailUrl: String?,
    fallbackColor: Color
): ColorClusters {
    if (thumbnailUrl.isNullOrEmpty()) {
        return ColorClusters(fallbackColor, fallbackColor, fallbackColor, fallbackColor, fallbackColor, fallbackColor)
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
        val lightVibrant = Color(palette.getLightVibrantColor(fallbackColor.toArgb()))
        val darkVibrant = Color(palette.getDarkVibrantColor(fallbackColor.toArgb()))
        val muted = Color(palette.getMutedColor(fallbackColor.toArgb()))
        val dominant = Color(palette.getDominantColor(fallbackColor.toArgb()))
        
        val surfaceColor = palette.vibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: fallbackColor.toArgb()
        
        return ColorClusters(vibrant, lightVibrant, darkVibrant, muted, dominant, Color(surfaceColor))
    }

    return ColorClusters(fallbackColor, fallbackColor, fallbackColor, fallbackColor, fallbackColor, fallbackColor)
}

fun Color.adaptToTheme(isDark: Boolean): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[1] = hsv[1].coerceAtMost(0.55f)

    return if (isDark) {
        hsv[2] = hsv[2].coerceIn(0.35f, 0.6f)
        Color(android.graphics.Color.HSVToColor((alpha * 255).toInt(), hsv))
    } else {
        hsv[2] = hsv[2].coerceIn(0.8f, 0.95f)
        Color(android.graphics.Color.HSVToColor((alpha * 255).toInt(), hsv))
    }
}
