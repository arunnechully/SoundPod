package com.github.soundpod.service

import com.github.innertube.Innertube
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
        visitorData?.let { Innertube.visitorData = it }
        poToken?.let { Innertube.poToken = it }
        cookies?.let { Innertube.cookies = it }
        decipher?.let { Innertube.decipher = it }
        
        if (Innertube.visitorData != null) {
            _isSessionReady.value = true
        }
    }
}
