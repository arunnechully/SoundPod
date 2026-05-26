package com.github.soundpod.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.github.soundpod.enums.AutoBackUp
import java.util.concurrent.TimeUnit

fun scheduleAutoBackup(context: Context, frequency: AutoBackUp) {
    val workManager = WorkManager.getInstance(context)
    val workName = "SoundPodAutoBackup"

    if (frequency == AutoBackUp.OFF) {
        workManager.cancelUniqueWork(workName)
        return
    }

    val intervalDays = when (frequency) {
        AutoBackUp.DAILY -> 1L
        AutoBackUp.WEEKLY -> 7L
        AutoBackUp.MONTHLY -> 30L
        AutoBackUp.OFF -> return
    }
    val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build()

    val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalDays, TimeUnit.DAYS)
        .setConstraints(constraints)
        .build()

    workManager.enqueueUniquePeriodicWork(
        workName,
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}