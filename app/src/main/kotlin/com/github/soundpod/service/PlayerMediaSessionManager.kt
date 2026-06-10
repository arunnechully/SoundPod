package com.github.soundpod.service

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.github.soundpod.R
import com.github.soundpod.utils.forceSeekToNext
import com.github.soundpod.utils.forceSeekToPrevious
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PlayerMediaSessionManager(
    context: Context,
    private val player: Player,
    private val coroutineScope: CoroutineScope,
    private val onPlayAction: () -> Unit,
    private val onLikeAction: () -> Unit,
    private val onLoopAction: () -> Unit
) {
    val mediaSession: MediaSession = MediaSession(context, "PlayerService")
    private val playbackStateMutex: Mutex = Mutex()
    private val metadataBuilder: MediaMetadata.Builder = MediaMetadata.Builder()
    private val stateBuilder: PlaybackState.Builder
        get() = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY
                        or PlaybackState.ACTION_PAUSE
                        or PlaybackState.ACTION_PLAY_PAUSE
                        or PlaybackState.ACTION_STOP
                        or PlaybackState.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackState.ACTION_SKIP_TO_NEXT
                        or PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM
                        or PlaybackState.ACTION_SEEK_TO
                        or PlaybackState.ACTION_REWIND
            )

    init {
        mediaSession.setCallback(SessionCallback())
        mediaSession.isActive = true
    }

    fun updatePlaybackState(isLiked: Boolean) {
        coroutineScope.launch {
            playbackStateMutex.withLock {
                withContext(Dispatchers.Main) {
                    val finalStateBuilder = stateBuilder
                        .addCustomAction(
                            FAVORITE_ACTION,
                            "Toggle like",
                            if (isLiked) R.drawable.heart else R.drawable.heart_outline
                        )
                        .addCustomAction(
                            LOOP_ACTION,
                            "Toggle loop",
                            when (player.repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                else -> R.drawable.repeat_off
                            }
                        )

                    mediaSession.setPlaybackState(
                        finalStateBuilder
                            .setState(player.androidPlaybackState, player.currentPosition, 1f)
                            .setBufferedPosition(player.bufferedPosition)
                            .build()
                    )
                }
            }
        }
    }

    fun updateMetadata(bitmapProvider: BitmapProvider, isAtLeastAndroid13: Boolean, isShowingThumbnailInLockscreen: Boolean) {
        try {
            if (player.duration != androidx.media3.common.C.TIME_UNSET) {
                metadataBuilder
                    .putText(MediaMetadata.METADATA_KEY_TITLE, player.mediaMetadata.title)
                    .putText(MediaMetadata.METADATA_KEY_ARTIST, player.mediaMetadata.artist)
                    .putText(MediaMetadata.METADATA_KEY_ALBUM, player.mediaMetadata.albumTitle)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, player.duration)
            }

            val bitmap = if (isAtLeastAndroid13 || isShowingThumbnailInLockscreen) {
                bitmapProvider.bitmap.let {
                    if (it.width > 512 || it.height > 512) {
                        Bitmap.createScaledBitmap(it, 512, 512, true)
                    } else it
                }
            } else null
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap)

            if (isAtLeastAndroid13 && player.currentMediaItemIndex == 0) {
                metadataBuilder.putText(MediaMetadata.METADATA_KEY_TITLE, "${player.mediaMetadata.title} ")
            }

            mediaSession.setMetadata(metadataBuilder.build())
        } catch (_: Exception) {
        }
    }

    fun updateQueue(timeline: Timeline) {
        val builder = MediaDescription.Builder()
        val currentMediaItemIndex = player.currentMediaItemIndex
        val lastIndex = timeline.windowCount - 1
        var startIndex = currentMediaItemIndex - 7
        var endIndex = currentMediaItemIndex + 7

        if (startIndex < 0) endIndex -= startIndex
        if (endIndex > lastIndex) {
            startIndex -= (endIndex - lastIndex)
            endIndex = lastIndex
        }

        startIndex = startIndex.coerceAtLeast(0)

        mediaSession.setQueue(
            List(endIndex - startIndex + 1) { index ->
                val mediaItem = timeline.getWindow(index + startIndex, Timeline.Window()).mediaItem
                MediaSession.QueueItem(
                    builder
                        .setMediaId(mediaItem.mediaId)
                        .setTitle(mediaItem.mediaMetadata.title)
                        .setSubtitle(mediaItem.mediaMetadata.artist)
                        .setIconUri(mediaItem.mediaMetadata.artworkUri)
                        .build(),
                    (index + startIndex).toLong()
                )
            }
        )
    }

    fun release() {
        mediaSession.isActive = false
        mediaSession.release()
    }

    private val Player.androidPlaybackState: Int
        get() = when (playbackState) {
            Player.STATE_BUFFERING -> if (playWhenReady) PlaybackState.STATE_BUFFERING else PlaybackState.STATE_PAUSED
            Player.STATE_READY -> if (playWhenReady) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackState.STATE_STOPPED
            Player.STATE_IDLE -> PlaybackState.STATE_NONE
            else -> PlaybackState.STATE_NONE
        }

    private inner class SessionCallback : MediaSession.Callback() {
        override fun onPlay() = onPlayAction()
        override fun onPause() = player.pause()
        override fun onSkipToPrevious() = runCatching(player::forceSeekToPrevious).let { }
        override fun onSkipToNext() = runCatching(player::forceSeekToNext).let { }
        override fun onSeekTo(pos: Long) = player.seekTo(pos)
        override fun onStop() = player.pause()
        override fun onRewind() = player.seekToDefaultPosition()
        override fun onSkipToQueueItem(id: Long) = runCatching { player.seekToDefaultPosition(id.toInt()) }.let { }

        override fun onCustomAction(action: String, extras: Bundle?) {
            super.onCustomAction(action, extras)
            when (action) {
                FAVORITE_ACTION -> onLikeAction()
                LOOP_ACTION -> onLoopAction()
            }
        }
    }

    companion object {
        const val FAVORITE_ACTION = "FAVORITE"
        const val LOOP_ACTION = "LOOP_ACTION"
    }
}