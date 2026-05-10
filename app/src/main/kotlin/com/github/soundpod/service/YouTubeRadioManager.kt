package com.github.soundpod.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.exoplayer.ExoPlayer
import com.github.innertube.models.NavigationEndpoint
import com.github.soundpod.utils.YouTubeRadio
import com.github.soundpod.utils.forcePlayFromBeginning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class YouTubeRadioManager(
    private val player: ExoPlayer,
    private val coroutineScope: CoroutineScope
) {
    private var radio: YouTubeRadio? = null
    private var radioJob: Job? = null
    
    var isLoadingRadio by mutableStateOf(false)
        private set

    fun setupRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) = startRadio(endpoint, true)
    fun playRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) = startRadio(endpoint, false)

    private fun startRadio(endpoint: NavigationEndpoint.Endpoint.Watch?, justAdd: Boolean) {
        radioJob?.cancel()
        radio = null
        val newRadio = YouTubeRadio(endpoint?.videoId, endpoint?.playlistId, endpoint?.playlistSetVideoId, endpoint?.params)
        
        isLoadingRadio = true
        radioJob = coroutineScope.launch(Dispatchers.Main) {
            val items = newRadio.process()
            if (justAdd) player.addMediaItems(items.drop(1)) 
            else player.forcePlayFromBeginning(items)
            
            radio = newRadio
            isLoadingRadio = false
        }
    }

    fun processNextBatch() {
        val radioInstance = radio ?: return
        if (player.mediaItemCount - player.currentMediaItemIndex <= 3) {
            coroutineScope.launch(Dispatchers.Main) {
                player.addMediaItems(radioInstance.process())
            }
        }
    }

    fun stop() {
        isLoadingRadio = false
        radioJob?.cancel()
        radio = null
    }
}