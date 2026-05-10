package com.github.soundpod.service

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.github.soundpod.R
import com.github.soundpod.utils.TimerJob
import com.github.soundpod.utils.timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class SleepTimerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val notificationManager: NotificationManager?,
    private val onTimerFinished: () -> Unit
) {
    private var timerJob: TimerJob? = null
    val millisLeft: StateFlow<Long?>? get() = timerJob?.millisLeft

    fun startTimer(delayMillis: Long) {
        timerJob?.cancel()
        timerJob = coroutineScope.timer(delayMillis) {
            val notification = NotificationCompat.Builder(context, PlayerNotificationManager.SLEEP_TIMER_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.sleep_timer_ended))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.app_icon)
                .build()

            notificationManager?.notify(PlayerNotificationManager.SLEEP_TIMER_NOTIFICATION_ID, notification)
            onTimerFinished()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}