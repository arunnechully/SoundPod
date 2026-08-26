package com.github.soundpod.service

import android.content.Context
import androidx.core.text.isDigitsOnly
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.github.soundpod.enums.ExoPlayerDiskCacheMaxSize
import com.github.soundpod.utils.exoPlayerDiskCacheMaxSizeKey
import com.github.soundpod.utils.getEnum
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerCacheManager(private val context: Context) {

    val cache: SimpleCache
    private val _cacheChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val cacheChanges = _cacheChanges.asSharedFlow()

    private inner class GlobalCacheListenerEvictor(
        private val delegate: CacheEvictor
    ) : CacheEvictor by delegate {

        override fun onSpanAdded(cache: Cache, span: CacheSpan) {
            delegate.onSpanAdded(cache, span)
            _cacheChanges.tryEmit(Unit)
        }

        override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
            delegate.onSpanRemoved(cache, span)
            _cacheChanges.tryEmit(Unit)
        }
    }

    init {
        val preferences = context.preferences
        val baseEvictor = when (val size =
            preferences.getEnum(exoPlayerDiskCacheMaxSizeKey, ExoPlayerDiskCacheMaxSize.`2GB`)) {
            ExoPlayerDiskCacheMaxSize.Unlimited -> NoOpCacheEvictor()
            else -> LeastRecentlyUsedCacheEvictor(size.bytes)
        }
        
        val globalEvictor = GlobalCacheListenerEvictor(baseEvictor)

        // Move cache from cacheDir to filesDir to prevent system clearing it when storage is low
        val oldDirectory = context.cacheDir.resolve("exoplayer")
        val directory = context.filesDir.resolve("exoplayer").also { directory ->
            if (!directory.exists()) {
                directory.mkdir()

                // Migrate from cacheDir if exists
                if (oldDirectory.exists()) {
                    oldDirectory.listFiles()?.forEach { file ->
                        file.renameTo(directory.resolve(file.name))
                    }
                    oldDirectory.deleteRecursively()
                }

                // Legacy migration
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
        
        cache = SimpleCache(directory, globalEvictor, StandaloneDatabaseProvider(context))
    }

    fun release() {
        cache.release()
    }

    fun isCached(videoId: String, position: Long, length: Long): Boolean {
        return cache.isCached(videoId, position, length)
    }

    fun removeCache(videoId: String) {
        cache.removeResource(videoId)
    }
}
