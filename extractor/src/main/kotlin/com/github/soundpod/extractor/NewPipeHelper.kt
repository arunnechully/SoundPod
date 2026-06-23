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
                println("SoundPod-Extractor: Initializing NewPipe with NewPipeDownloader")
                org.schabi.newpipe.extractor.NewPipe.init(NewPipeDownloader.getInstance())
            } catch (e: Exception) {
                println("SoundPod-Extractor: Failed to initialize NewPipe: ${e.message}")
                e.printStackTrace()
                isInitialized.set(false)
            }
        }
    }
}
