package com.github.soundpod.extractor

import java.util.concurrent.atomic.AtomicBoolean

object NewPipeHelper {
    private val isInitialized = AtomicBoolean(false)
    
    val isLibraryAvailable: Boolean by lazy {
        try {
            Class.forName("org.schabi.newpipe.extractor.NewPipe")
            true
        } catch (_: Exception) {
            false
        }
    }

    fun init() {
        if (isLibraryAvailable && isInitialized.compareAndSet(false, true)) {
            try {
                org.schabi.newpipe.extractor.NewPipe.init(KtorDownloader())
            } catch (_: Exception) {
                isInitialized.set(false)
            }
        }
    }
}
