package com.github.soundpod.service

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
import coil3.request.bitmapConfig
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import com.github.soundpod.utils.thumbnail

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

    private var isLoading = false
    private val pendingCallbacks = mutableListOf<(Bitmap) -> Unit>()

    fun load(uri: Uri?, onDone: (Bitmap) -> Unit) {
        if (lastUri == uri && uri != null) {
            if (lastBitmap != null) {
                onDone(lastBitmap!!)
            } else if (isLoading) {
                pendingCallbacks.add(onDone)
            } else {
                onDone(defaultBitmap)
            }
            return
        }

        lastEnqueued?.dispose()
        lastUri = uri
        lastBitmap = null
        isLoading = true
        pendingCallbacks.clear()
        pendingCallbacks.add(onDone)

        if (uri == null) {
            isLoading = false
            onDone(defaultBitmap)
            listener?.invoke(null)
            return
        }

        lastEnqueued = context.applicationContext.imageLoader.enqueue(
            ImageRequest.Builder(context.applicationContext)
                .data(uri.thumbnail(bitmapSize))
                .size(bitmapSize)
                .precision(Precision.EXACT)
                .scale(Scale.FILL)
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .allowHardware(false)
                .listener(
                    onError = { _, _ ->
                        isLoading = false
                        val callbacks = ArrayList(pendingCallbacks)
                        pendingCallbacks.clear()
                        callbacks.forEach { it(defaultBitmap) }
                        listener?.invoke(null)
                    },
                    onSuccess = { _, result ->
                        isLoading = false
                        lastBitmap = result.image.toBitmap()
                        val callbacks = ArrayList(pendingCallbacks)
                        pendingCallbacks.clear()
                        callbacks.forEach { it(lastBitmap!!) }
                        listener?.invoke(lastBitmap)
                    }
                )
                .build()
        )
    }
}
