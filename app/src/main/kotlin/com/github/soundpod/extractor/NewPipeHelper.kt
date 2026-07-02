package com.github.soundpod.extractor

import com.github.soundpod.NewPipeDownloader
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
                val localization = org.schabi.newpipe.extractor.localization.Localization.fromLocale(java.util.Locale.getDefault())
                val contentCountry = org.schabi.newpipe.extractor.localization.ContentCountry(java.util.Locale.getDefault().country)
                
                org.schabi.newpipe.extractor.NewPipe.init(
                    NewPipeDownloader.getInstance(),
                    localization,
                    contentCountry
                )
            } catch (e: Exception) {
                println("SoundPod-Extractor: Failed to initialize NewPipe: ${e.message}")
                e.printStackTrace()
                isInitialized.set(false)
            }
        }
    }
}
