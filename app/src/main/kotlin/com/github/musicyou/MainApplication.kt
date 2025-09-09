package com.github.musicyou

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import com.github.innertube.Innertube
import com.github.innertube.requests.visitorData
import com.github.musicyou.enums.CoilDiskCacheMaxSize
import com.github.musicyou.utils.coilDiskCacheMaxSizeKey
import com.github.musicyou.utils.getEnum
import com.github.musicyou.utils.preferences
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainApplication : Application(), SingletonImageLoader.Factory {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        DatabaseInitializer()

        GlobalScope.launch {
            if (Innertube.visitorData.isNullOrBlank()) Innertube.visitorData =
                Innertube.visitorData().getOrNull()
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
}