package com.github.soundpod

import android.app.Application
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainApplication : Application(), SingletonImageLoader.Factory {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        DatabaseInitializer.get(this)
        fetchYouTubeSession()
    }

    private fun fetchYouTubeSession() {
        appScope.launch {
            try {
                if (Innertube.visitorData.isNullOrBlank()) {
                    Log.d("SoundPodApp", "Fetching fresh YouTube session token...")

                    val result = Innertube.visitorData()

                    result.onSuccess { token ->
                        Innertube.visitorData = token
                        Log.d("SoundPodApp", "Session secured successfully.")
                    }.onFailure { error ->
                        Log.e("SoundPodApp", "Failed to fetch session: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SoundPodApp", "Unexpected error during session init: ${e.message}")
            }
        }
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