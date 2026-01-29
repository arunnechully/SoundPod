package com.github.soundpod

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import com.github.innertube.Innertube
import com.github.innertube.requests.visitorData
import com.github.soundpod.enums.CoilDiskCacheMaxSize
import com.github.soundpod.utils.coilDiskCacheMaxSizeKey
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        instance = this // Fixes the Unresolved appContext error

        // Initialize the database singleton safely
        DatabaseInitializer.get(this)

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            if (Innertube.visitorData.isNullOrBlank()) {
                Innertube.visitorData = Innertube.visitorData().getOrNull()
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .diskCache(
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil"))
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
        private lateinit var instance: MainApplication
        // This is the specific property the Database file will look for
        val appContext: Context get() = instance.applicationContext
    }
}