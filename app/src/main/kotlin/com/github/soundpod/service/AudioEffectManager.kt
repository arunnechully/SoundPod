package com.github.soundpod.service

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.exoplayer.ExoPlayer
import com.github.soundpod.db
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.volumeNormalizationKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AudioEffectManager(
    private val context: Context,
    private val player: ExoPlayer,
    private val coroutineScope: CoroutineScope
) {
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var volumeNormalizationJob: Job? = null

    fun maybeNormalizeVolume() {
        if (!context.preferences.getBoolean(volumeNormalizationKey, false)) {
            loudnessEnhancer?.enabled = false
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            volumeNormalizationJob?.cancel()
            player.volume = 1f
            return
        }

        if (loudnessEnhancer == null) {
            loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        }

        player.currentMediaItem?.mediaId?.let { songId ->
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = coroutineScope.launch(Dispatchers.Main) {
                db.loudnessDb(songId).cancellable().collectLatest { loudnessDb ->
                    try {
                        loudnessEnhancer?.setTargetGain(-((loudnessDb ?: 0f) * 100).toInt() + 500)
                        loudnessEnhancer?.enabled = true
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    fun sendOpenEqualizerIntent() {
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }

    fun sendCloseEqualizerIntent() {
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
            }
        )
    }

    fun release() {
        loudnessEnhancer?.release()
        volumeNormalizationJob?.cancel()
    }
}