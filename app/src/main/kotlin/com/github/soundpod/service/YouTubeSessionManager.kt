package com.github.soundpod.service

import com.github.innertube.Innertube
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object YouTubeSessionManager {
    private val _isSessionReady = MutableStateFlow(false)
    val isSessionReady = _isSessionReady.asStateFlow()

    private val _isBootstrapped = MutableStateFlow(false)
    val isBootstrapped = _isBootstrapped.asStateFlow()

    fun updateSession(
        visitorData: String? = null,
        jsUrl: String? = null,
        isFromBootstrap: Boolean = false
    ) {
        visitorData?.let { Innertube.visitorData = it }

        jsUrl?.let {
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            scope.launch { YouTubeDecipherer.initialize(it) }
        }

        if (Innertube.visitorData != null) {
            _isSessionReady.value = true
        }

        if (isFromBootstrap && Innertube.visitorData != null) {
            _isBootstrapped.value = true
        }
    }
}
