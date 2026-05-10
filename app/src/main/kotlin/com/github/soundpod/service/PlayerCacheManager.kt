package com.github.soundpod.service

import android.content.Context
import androidx.core.text.isDigitsOnly
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.github.soundpod.enums.ExoPlayerDiskCacheMaxSize
import com.github.soundpod.utils.exoPlayerDiskCacheMaxSizeKey
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.preferences

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerCacheManager(private val context: Context) {

    val cache: SimpleCache

    init {
        val preferences = context.preferences
        val cacheEvictor = when (val size =
            preferences.getEnum(exoPlayerDiskCacheMaxSizeKey, ExoPlayerDiskCacheMaxSize.`2GB`)) {
            ExoPlayerDiskCacheMaxSize.Unlimited -> NoOpCacheEvictor()
            else -> LeastRecentlyUsedCacheEvictor(size.bytes)
        }

        // TODO: Remove in a future release
        val directory = context.cacheDir.resolve("exoplayer").also { directory ->
            if (!directory.exists()) {
                directory.mkdir()

                context.cacheDir.listFiles()?.forEach { file ->
                    if (file.isDirectory && file.name.length == 1 && file.name.isDigitsOnly() || file.extension == "uid") {
                        if (!file.renameTo(directory.resolve(file.name))) {
                            file.deleteRecursively()
                        }
                    }
                }

                context.filesDir.resolve("coil").deleteRecursively()
            }
        }
        
        cache = SimpleCache(directory, cacheEvictor, StandaloneDatabaseProvider(context))
    }

    fun release() {
        cache.release()
    }
}