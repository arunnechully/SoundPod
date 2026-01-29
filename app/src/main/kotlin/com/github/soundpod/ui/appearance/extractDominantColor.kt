package com.github.soundpod.ui.appearance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
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

// 1. Add 'defaultColor' parameter
suspend fun extractDominantColor(
    context: Context,
    imageUrl: String?,
    defaultColor: Color
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

// ... (Keep asBitmap extension same as before) ...
fun Image.asBitmap(context: Context): Bitmap {
    val drawable = this.asDrawable(context.resources)
    if (drawable is BitmapDrawable) return drawable.bitmap

    val bitmap = createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1)
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}