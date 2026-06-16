package com.github.soundpod.service

import com.github.innertube.Innertube
import com.github.soundpod.MainApplication
import com.github.soundpod.utils.preferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object YouTubeSessionManager {
    private val _isSessionReady = MutableStateFlow(false)
    val isSessionReady = _isSessionReady.asStateFlow()

    fun updateSession(
        visitorData: String? = null,
        poToken: String? = null,
        cookies: String? = null,
        decipher: (suspend (String) -> String)? = null
    ) {
        val prefs = MainApplication.appContext.preferences
        
        visitorData?.let { Innertube.visitorData = it }
        poToken?.let { Innertube.poToken = it }
        
        cookies?.let { 
            Innertube.cookies = it
            prefs.edit { putString("cookies", it) }
        }
        
        decipher?.let { Innertube.decipher = it }
        
        if (Innertube.visitorData != null) {
            _isSessionReady.value = true
        }
    }
}
