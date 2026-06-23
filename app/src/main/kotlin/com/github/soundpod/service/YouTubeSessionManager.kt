package com.github.soundpod.service

import com.github.innertube.Innertube
import com.github.soundpod.MainApplication
import com.github.soundpod.utils.preferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object YouTubeSessionManager {
    private val _isSessionReady = MutableStateFlow(false)
    val isSessionReady = _isSessionReady.asStateFlow()

    private val _isBootstrapped = MutableStateFlow(false)
    val isBootstrapped = _isBootstrapped.asStateFlow()

    private val _needsConsent = MutableStateFlow(false)
    val needsConsent = _needsConsent.asStateFlow()

    fun setNeedsConsent(value: Boolean) {
        _needsConsent.value = value
    }

    fun updateSession(
        visitorData: String? = null,
        poToken: String? = null,
        apiKey: String? = null,
        clientName: String? = null,
        clientVersion: String? = null,
        jsUrl: String? = null,
        cookies: String? = null,
        decipher: (suspend (String) -> String)? = null,
        signatureDecipher: (suspend (String) -> String)? = null,
        isFromBootstrap: Boolean = false
    ) {
        val prefs = MainApplication.appContext.preferences
        
        visitorData?.let { Innertube.visitorData = it }
        poToken?.let { Innertube.poToken = it }
        apiKey?.let { Innertube.apiKey = it }
        clientName?.let { Innertube.clientName = it }
        clientVersion?.let { Innertube.clientVersion = it }
        
        jsUrl?.let { 
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            scope.launch { YouTubeDecipherer.initialize(it) }
        }
        
        cookies?.let { 
            Innertube.cookies = it
            // Also store in preferences for persistence
            MainApplication.appContext.preferences.edit { putString("cookies", it) }
        }
        
        decipher?.let { Innertube.decipher = it }
        signatureDecipher?.let { Innertube.signatureDecipher = it }
        
        if (Innertube.visitorData != null) {
            _isSessionReady.value = true
        }

        if (isFromBootstrap && Innertube.visitorData != null && Innertube.apiKey != null) {
            _isBootstrapped.value = true
        }
    }
}
