package com.github.soundpod

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.request.crossfade
import com.github.innertube.Innertube
import com.github.soundpod.extractor.NewPipeDownloader
import com.github.soundpod.enums.CoilDiskCacheMaxSize
import com.github.soundpod.utils.coilDiskCacheMaxSizeKey
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.preferences
import com.github.soundpod.service.YouTubeBootstrap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale


class MainApplication : Application(), SingletonImageLoader.Factory {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Locale.setDefault(Locale.US)
        NewPipeDownloader.init(cacheDir)

        Thread {
            DatabaseInitializer.get(this)

            Innertube.visitorData = preferences.getString("visitor_data", null)
            Innertube.onVisitorDataChanged = { visitorData: String? ->
                preferences.edit { putString("visitor_data", visitorData) }
            }

            
            // Trigger dynamic session bootstrap
            applicationScope.launch {
                YouTubeBootstrap.initialize()
            }
        }.start()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
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