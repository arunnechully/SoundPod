package com.github.soundpod.viewmodels

import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.github.soundpod.utils.shouldBePlaying
import com.github.soundpod.utils.windows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PlaylistViewModel(private val player: Player) : ViewModel() {

    data class PlaylistUiState(
        val windows: List<Timeline.Window> = emptyList(),
        val currentIndex: Int = -1,
        val isPlaying: Boolean = false
    )

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_PLAYBACK_STATE_CHANGED
                )
            ) {
                updateState()
            }
        }
    }

    init {
        player.addListener(listener)
        updateState()
    }

    private fun updateState() {
        _uiState.update {
            it.copy(
                windows = player.currentTimeline.windows,
                currentIndex = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex,
                isPlaying = player.shouldBePlaying
            )
        }
    }

    fun handleItemClick(index: Int) {
        if (player.currentMediaItemIndex == index) {
            if (player.shouldBePlaying) player.pause() else player.play()
        } else {
            player.seekToDefaultPosition(index)
            player.playWhenReady = true
        }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        player.moveMediaItem(fromIndex, toIndex)
    }

    fun clearQueue() {
        player.clearMediaItems()
    }

    override fun onCleared() {
        player.removeListener(listener)
        super.onCleared()
    }
}