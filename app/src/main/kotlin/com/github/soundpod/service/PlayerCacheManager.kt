package com.github.soundpod.service

import android.content.Context
import androidx.core.text.isDigitsOnly
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.github.soundpod.enums.DownloadDiskCacheMaxSize
import com.github.soundpod.enums.ExoPlayerDiskCacheMaxSize
import com.github.soundpod.utils.downloadDiskCacheMaxSizeKey
import com.github.soundpod.utils.exoPlayerDiskCacheMaxSizeKey
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.preferences

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerCacheManager(private val context: Context) {

    val cache: SimpleCache
    val downloadCache: SimpleCache

    init {
        val preferences = context.preferences
        val databaseProvider = StandaloneDatabaseProvider(context)

        val cacheEvictor = when (val size =
            preferences.getEnum(exoPlayerDiskCacheMaxSizeKey, ExoPlayerDiskCacheMaxSize.`2GB`)) {
            ExoPlayerDiskCacheMaxSize.Unlimited -> NoOpCacheEvictor()
            else -> LeastRecentlyUsedCacheEvictor(size.bytes)
        }

        val downloadCacheEvictor = when (val size =
            preferences.getEnum(downloadDiskCacheMaxSizeKey, DownloadDiskCacheMaxSize.`2GB`)) {
            DownloadDiskCacheMaxSize.Unlimited -> NoOpCacheEvictor()
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

        val downloadDirectory = context.filesDir.resolve("downloads").also {
            if (!it.exists()) it.mkdir()
        }
        
        cache = SimpleCache(directory, cacheEvictor, databaseProvider)
        downloadCache = SimpleCache(downloadDirectory, downloadCacheEvictor, databaseProvider)
    }

    fun release() {
        cache.release()
        downloadCache.release()
    }

    fun isCached(videoId: String, position: Long, length: Long): Boolean {
        if (videoId.isBlank() || position < 0 || length <= 0L) return false
        return cache.isCached(videoId, position, length) || downloadCache.isCached(videoId, position, length)
    }

    fun removeCache(videoId: String) {
        cache.removeResource(videoId)
        downloadCache.removeResource(videoId)
        context.filesDir.resolve("songs").resolve(videoId).delete()
    }
}
