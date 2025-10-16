package com.soundpod.music.player

import android.app.PendingIntent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize ExoPlayer
        player = ExoPlayer.Builder(this).build()

        // 2. Build the PendingIntent for the notification's click action
        // Handle potential null from getLaunchIntentForPackage
        val activityIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let {
                // Use FLAG_IMMUTABLE
                PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
            }

        // 3. Create the MediaSession, linking it to the player
        val sessionBuilder = MediaSession.Builder(this, player!!) // player is non-null here from step 1

        // Safely set session activity only if the PendingIntent was created
        activityIntent?.let { intent ->
            sessionBuilder.setSessionActivity(intent)
        }

        mediaSession = sessionBuilder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        // Safely release the player and session instances
        player?.release()
        player = null

        mediaSession?.release()
        mediaSession = null

        super.onDestroy()
    }

    fun playMedia(uri: Uri, title: String? = null, artist: String? = null) {
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title ?: "Unknown Title")
                    .setArtist(artist ?: "Unknown Artist")
                    .build()
            )
            .build()

        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

}