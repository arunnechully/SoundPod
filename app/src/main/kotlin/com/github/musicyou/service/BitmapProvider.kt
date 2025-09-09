package com.github.musicyou.service

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.github.musicyou.utils.thumbnail

class BitmapProvider(
    private val context: Context,
    private val bitmapSize: Int,
    private val colorProvider: (isSystemInDarkMode: Boolean) -> Int
) {
    var lastUri: Uri? = null
        private set

    var lastBitmap: Bitmap? = null
    private var lastIsSystemInDarkMode = false

    private var lastEnqueued: Disposable? = null

    private lateinit var defaultBitmap: Bitmap

    val bitmap: Bitmap
        get() = lastBitmap ?: defaultBitmap

    var listener: ((Bitmap?) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(lastBitmap)
        }

    init {
        setDefaultBitmap()
    }

    fun setDefaultBitmap(): Boolean {
        val isSystemInDarkMode = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        var previousBitmap: Bitmap? = null

        if (::defaultBitmap.isInitialized) {
            if (isSystemInDarkMode == lastIsSystemInDarkMode) return false
            previousBitmap = defaultBitmap
        }

        lastIsSystemInDarkMode = isSystemInDarkMode

        defaultBitmap = createBitmap(bitmapSize, bitmapSize).applyCanvas {
            drawColor(colorProvider(isSystemInDarkMode))
        }
        previousBitmap?.recycle()

        return lastBitmap == null
    }

    fun load(uri: Uri?, onDone: (Bitmap) -> Unit) {
        if (lastUri == uri) return

        lastEnqueued?.dispose()
        lastUri = uri

        lastEnqueued = context.applicationContext.imageLoader.enqueue(
            ImageRequest.Builder(context.applicationContext)
                .data(uri.thumbnail(bitmapSize))
                .allowHardware(false)
                .listener(
                    onError = { _, _ ->
                        lastBitmap = null
                        onDone(bitmap)
                        listener?.invoke(lastBitmap)
                    },
                    onSuccess = { _, result ->
                        lastBitmap = result.image.toBitmap()
                        onDone(bitmap)
                        listener?.invoke(lastBitmap)
                    }
                )
                .build()
        )
    }
}