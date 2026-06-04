package com.github.soundpod.ui.appearance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
import coil3.Image
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun extractDominantColor(
    context: Context,
    imageUrl: String?,
    defaultColor: Color,
): Color {

    if (imageUrl.isNullOrEmpty()) return defaultColor

    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .size(128)
                .precision(Precision.EXACT)
                .build()

            val result = loader.execute(request)

            if (result is SuccessResult) {
                val bitmap = result.image.asBitmap(context)
                val palette = Palette.from(bitmap).generate()

                val swatch = palette.vibrantSwatch
                    ?: palette.dominantSwatch
                    ?: palette.mutedSwatch

                if (swatch != null) Color(swatch.rgb) else defaultColor
            } else {
                defaultColor
            }
        } catch (_: Exception) {
            defaultColor
        }
    }
}

data class ColorClusters(
    val c1: Color,
    val c2: Color,
    val c3: Color
)

private val FallbackColors = listOf(
    Color(0xFF6200EE), // Purple
    Color(0xFF03DAC6), // Teal
    Color(0xFF3700B3)  // Deep Blue
)

suspend fun extractColorClusters(
    context: Context,
    imageUrl: String?,
    defaultColor: Color,
): ColorClusters {
    if (imageUrl.isNullOrEmpty()) return ColorClusters(defaultColor, defaultColor, defaultColor)

    return withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .size(128)
                .precision(Precision.EXACT)
                .build()

            val result = loader.execute(request)

            if (result is SuccessResult) {
                val bitmap = result.image.asBitmap(context)
                val palette = Palette.from(bitmap).generate()

                // Get 3 distinct colors from the palette
                var c1 = palette.vibrantSwatch?.rgb?.let { Color(it) }
                    ?: palette.dominantSwatch?.rgb?.let { Color(it) }
                    ?: defaultColor

                var c2 = palette.mutedSwatch?.rgb?.let { Color(it) }
                    ?: palette.lightVibrantSwatch?.rgb?.let { Color(it) }
                    ?: palette.darkVibrantSwatch?.rgb?.let { Color(it) }
                    ?: defaultColor

                var c3 = palette.lightMutedSwatch?.rgb?.let { Color(it) }
                    ?: palette.darkMutedSwatch?.rgb?.let { Color(it) }
                    ?: palette.dominantSwatch?.rgb?.let { Color(it) }
                    ?: defaultColor

                // Check if the overall palette is too dark (e.g., black thumbnail)
                val avgLuminance = (c1.luminance() + c2.luminance() + c3.luminance()) / 3f
                if (avgLuminance < 0.05f) {
                    // Use pleasant fallback colors if the image is too dark
                    c1 = FallbackColors[0]
                    c2 = FallbackColors[1]
                    c3 = FallbackColors[2]
                }

                ColorClusters(c1, c2, c3)
            } else {
                ColorClusters(defaultColor, defaultColor, defaultColor)
            }
        } catch (_: Exception) {
            ColorClusters(defaultColor, defaultColor, defaultColor)
        }
    }
}

fun Image.asBitmap(context: Context): Bitmap {
    val drawable = this.asDrawable(context.resources)
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }

    val bitmap = createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1)
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
