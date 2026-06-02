package com.github.soundpod.service

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.github.soundpod.R
import com.github.soundpod.utils.TimerJob
import com.github.soundpod.utils.timer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SleepTimerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val notificationManager: NotificationManager?,
    private val onTimerFinished: () -> Unit
) {
    private var timerJob: TimerJob? = null
    private var collectionJob: Job? = null
    
    private val _millisLeft = MutableStateFlow<Long?>(null)
    val millisLeft: StateFlow<Long?> = _millisLeft.asStateFlow()

    fun startTimer(delayMillis: Long) {
        timerJob?.cancel()
        collectionJob?.cancel()
        
        timerJob = coroutineScope.timer(delayMillis) {
            val notification = NotificationCompat.Builder(context, PlayerNotificationManager.SLEEP_TIMER_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.sleep_timer_ended))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.app_icon)
                .build()

            notificationManager?.notify(PlayerNotificationManager.SLEEP_TIMER_NOTIFICATION_ID, notification)
            onTimerFinished()
            _millisLeft.value = null
        }
        
        collectionJob = coroutineScope.launch {
            timerJob?.millisLeft?.collect {
                _millisLeft.value = it
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        collectionJob?.cancel()
        collectionJob = null
        _millisLeft.value = null
    }
}
