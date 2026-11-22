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

    fun setupRadio(
        endpoint: NavigationEndpoint.Endpoint.Watch?,
        title: String? = null,
        artist: String? = null
    ) = startRadio(endpoint, true, title, artist)

    fun playRadio(
        endpoint: NavigationEndpoint.Endpoint.Watch?,
        title: String? = null,
        artist: String? = null
    ) = startRadio(endpoint, false, title, artist)

    private fun startRadio(
        endpoint: NavigationEndpoint.Endpoint.Watch?,
        justAdd: Boolean,
        title: String? = null,
        artist: String? = null
    ) {
        radioJob?.cancel()
        radio = null
        val songTitle = title ?: player.currentMediaItem?.mediaMetadata?.title?.toString()
        val songArtist = artist ?: player.currentMediaItem?.mediaMetadata?.artist?.toString()

        val newRadio = YouTubeRadio(
            videoId = endpoint?.videoId,
            playlistId = endpoint?.playlistId,
            playlistSetVideoId = endpoint?.playlistSetVideoId,
            parameters = endpoint?.params,
            title = songTitle,
            artist = songArtist
        )
        
        isLoadingRadio = true
        radioJob = coroutineScope.launch(Dispatchers.Main) {
            val items = newRadio.process()
            val existingIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }.toSet()
            val newItems = items.drop(1).filter { it.mediaId !in existingIds }

            if (justAdd) {
                if (newItems.isNotEmpty()) {
                    player.addMediaItems(newItems)
                }
            } else {
                player.forcePlayFromBeginning(items)
            }
            
            radio = newRadio
            isLoadingRadio = false
        }
    }

    fun processNextBatch() {
        val radioInstance = radio ?: return
        if (player.mediaItemCount - player.currentMediaItemIndex <= 3) {
            coroutineScope.launch(Dispatchers.Main) {
                val nextBatch = radioInstance.process()
                val existingIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }.toSet()
                val newItems = nextBatch.filter { it.mediaId !in existingIds }
                if (newItems.isNotEmpty()) {
                    player.addMediaItems(newItems)
                }
            }
        }
    }

    fun stop() {
        isLoadingRadio = false
        radioJob?.cancel()
        radio = null
    }
}
