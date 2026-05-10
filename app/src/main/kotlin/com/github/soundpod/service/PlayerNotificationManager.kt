package com.github.soundpod.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.media3.common.Player
import com.github.soundpod.MainActivity
import com.github.soundpod.R
import com.github.soundpod.utils.activityPendingIntent
import com.github.soundpod.utils.broadCastPendingIntent
import com.github.soundpod.utils.forceSeekToNext
import com.github.soundpod.utils.forceSeekToPrevious
import com.github.soundpod.utils.intent
import com.github.soundpod.utils.shouldBePlaying

class PlayerNotificationManager(
    private val context: Context,
    private val player: Player,
    private val mediaSessionToken: MediaSessionCompat.Token,
    private val bitmapProvider: BitmapProvider,
    private val onCoverArtReady: () -> Unit,
    onPlayAction: () -> Unit
) {
    val notificationManager: NotificationManager? = context.getSystemService()
    private val notificationActionReceiver = NotificationActionReceiver(player, onPlayAction)

    init {
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction(Action.play.value)
            addAction(Action.pause.value)
            addAction(Action.next.value)
            addAction(Action.previous.value)
        }
        ContextCompat.registerReceiver(
            context,
            notificationActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun getNotification(): Notification? {
        if (player.currentMediaItem == null) return null

        val playIntent = Action.play.pendingIntent(context)
        val pauseIntent = Action.pause.pendingIntent(context)
        val nextIntent = Action.next.pendingIntent(context)
        val prevIntent = Action.previous.pendingIntent(context)

        val mediaMetadata = player.mediaMetadata

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(mediaMetadata.title)
            .setContentText(mediaMetadata.artist)
            .setSubText(player.playerError?.message)
            .setLargeIcon(bitmapProvider.bitmap)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(player.playerError?.let { R.drawable.alert_circle } ?: R.drawable.app_icon)
            .setOngoing(false)
            .setContentIntent(
                context.activityPendingIntent<MainActivity>(
                    flags = PendingIntent.FLAG_UPDATE_CURRENT
                ) { putExtra("expandPlayerBottomSheet", true) }
            )
            .setDeleteIntent(context.broadCastPendingIntent<NotificationDismissReceiver>())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSessionToken)
            )
            .addAction(R.drawable.play_skip_back, "Skip back", prevIntent)
            .addAction(
                if (player.shouldBePlaying) R.drawable.pause else R.drawable.play,
                if (player.shouldBePlaying) "Pause" else "Play",
                if (player.shouldBePlaying) pauseIntent else playIntent
            )
            .addAction(R.drawable.play_skip_forward, "Skip forward", nextIntent)

        bitmapProvider.load(mediaMetadata.artworkUri) { bitmap ->
            onCoverArtReady()
            notificationManager?.notify(NOTIFICATION_ID, builder.setLargeIcon(bitmap).build())
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        notificationManager?.run {
            if (getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        context.getString(R.string.now_playing),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }

            if (getNotificationChannel(SLEEP_TIMER_NOTIFICATION_CHANNEL_ID) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        SLEEP_TIMER_NOTIFICATION_CHANNEL_ID,
                        context.getString(R.string.sleep_timer),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        setSound(null, null)
                        enableLights(false)
                        enableVibration(false)
                    }
                )
            }
        }
    }

    fun release() {
        context.unregisterReceiver(notificationActionReceiver)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "default_channel_id"
        const val SLEEP_TIMER_NOTIFICATION_ID = 1002
        const val SLEEP_TIMER_NOTIFICATION_CHANNEL_ID = "sleep_timer_channel_id"
    }
}

@JvmInline
private value class Action(val value: String) {
    fun pendingIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        100,
        Intent(value).setPackage(ctx.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        val pause = Action("com.github.soundpod.pause")
        val play = Action("com.github.soundpod.play")
        val next = Action("com.github.soundpod.next")
        val previous = Action("com.github.soundpod.previous")
    }
}

private class NotificationActionReceiver(
    private val player: Player,
    private val onPlay: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Action.pause.value -> player.pause()
            Action.play.value -> onPlay()
            Action.next.value -> player.forceSeekToNext()
            Action.previous.value -> player.forceSeekToPrevious()
        }
    }
}

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.stopService(context.intent<PlayerService>())
    }
}