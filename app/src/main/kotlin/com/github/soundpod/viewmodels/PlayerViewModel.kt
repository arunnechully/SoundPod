package com.github.soundpod.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.soundpod.db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val mediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val playbackState: Int = Player.STATE_IDLE,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val artistId: String? = null
)

@UnstableApi
class PlayerViewModel(
    private val player: Player
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState = _uiState.asStateFlow()

    private var progressTrackerJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _uiState.update { it.copy(mediaItem = mediaItem) }
            fetchArtistInfo(mediaItem)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            manageProgressTracker(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update { it.copy(playbackState = playbackState) }
        }

        override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
            _uiState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                events.contains(Player.EVENT_TIMELINE_CHANGED)
            ) {
                _uiState.update {
                    it.copy(
                        currentPositionMs = player.currentPosition,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
            }
        }
    }

    init {
        player.addListener(playerListener)

        _uiState.update {
            it.copy(
                mediaItem = player.currentMediaItem,
                isPlaying = player.isPlaying,
                playbackState = player.playbackState,
                playbackSpeed = player.playbackParameters.speed,
                currentPositionMs = player.currentPosition,
                durationMs = player.duration.coerceAtLeast(0L)
            )
        }

        fetchArtistInfo(player.currentMediaItem)
        manageProgressTracker(player.isPlaying)
    }
    private var dbJob: Job? = null
    private fun fetchArtistInfo(mediaItem: MediaItem?) {
        dbJob?.cancel()
        if (mediaItem == null) {
            _uiState.update { it.copy(artistId = null) }
            return
        }
        dbJob = viewModelScope.launch(Dispatchers.IO) {
            val artistsInfo = db.songArtistInfo(mediaItem.mediaId)
            val id = if (artistsInfo.size == 1) artistsInfo.first().id else null

            _uiState.update { it.copy(artistId = id) }
        }
    }

    private fun manageProgressTracker(isPlaying: Boolean) {
        progressTrackerJob?.cancel()
        if (isPlaying) {
            progressTrackerJob = viewModelScope.launch {
                while (isActive) {
                    _uiState.update { it.copy(currentPositionMs = player.currentPosition) }
                    delay(250)
                }
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            when (player.playbackState) {
                Player.STATE_IDLE -> player.prepare()
                Player.STATE_ENDED -> player.seekToDefaultPosition(0)
                Player.STATE_BUFFERING,
                Player.STATE_READY -> { /* Already prepared, just play */ }
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun skipToNext() {
        player.seekToNext()
    }

    fun skipToPrevious() {
        player.seekToPrevious()
    }

    override fun onCleared() {
        super.onCleared()
        player.removeListener(playerListener)
        progressTrackerJob?.cancel()
    }
}