package com.github.soundpod

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.intercept.Interceptor
import coil3.request.CachePolicy
import coil3.request.ImageResult
import coil3.request.crossfade
import com.github.innertube.Innertube
import com.github.soundpod.extractor.NewPipeHelper
import com.github.soundpod.enums.CoilDiskCacheMaxSize
import com.github.soundpod.utils.coilDiskCacheMaxSizeKey
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.pauseImageCacheKey
import com.github.soundpod.utils.preferences
import java.util.Locale


class MainApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Locale.setDefault(Locale.US)
        NewPipeHelper.init()

        Thread {
            DatabaseInitializer.get(this)

            Innertube.visitorData = preferences.getString("visitor_data", null)
            Innertube.onVisitorDataChanged = { visitorData: String? ->
                preferences.edit { putString("visitor_data", visitorData) }
            }
        }.start()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .components {
                add(object : Interceptor {
                    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                        val pauseImageCache = preferences.getBoolean(pauseImageCacheKey, false)
                        val request = if (pauseImageCache) {
                            chain.request.newBuilder()
                                .diskCachePolicy(CachePolicy.READ_ONLY)
                                .build()
                        } else {
                            chain.request
                        }
                        return chain.withRequest(request).proceed()
                    }
                })
            }
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes(
                        preferences.getEnum(
                            coilDiskCacheMaxSizeKey,
                            CoilDiskCacheMaxSize.`128MB`
                        ).bytes
                    )
                    .build()
            )
            .build()
    }

    companion object {
        private var instance: MainApplication? = null
        val appContext: Context get() = instance!!.applicationContext
    }
}