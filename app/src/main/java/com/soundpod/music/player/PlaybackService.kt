package com.soundpod.music.player

import android.app.PendingIntent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()

        val activityIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let {
                PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
            }
        val sessionBuilder = MediaSession.Builder(this, player!!)

        activityIntent?.let { intent ->
            sessionBuilder.setSessionActivity(intent)
        }

        mediaSession = sessionBuilder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {

        player?.release()
        player = null

        mediaSession?.release()
        mediaSession = null

        super.onDestroy()
    }


}