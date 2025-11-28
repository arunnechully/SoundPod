package com.github.soundpod.ui.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.api.GitHub
import com.github.soundpod.MainActivity
import com.github.soundpod.R
import com.github.soundpod.utils.VersionUtils
import kotlinx.coroutines.flow.first

class UpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val isAutoCheckEnabled = autoCheckEnabled(context).first()
        val isShowAlertEnabled = showUpdateAlert(context).first()

        if (!isAutoCheckEnabled || !isShowAlertEnabled) {
            return Result.success()
        }

        return try {
            val release = GitHub.getLastestRelease() ?: return Result.failure()

            val latestVersionStr = release.name
            val latestVersion = VersionUtils.extractVersion(latestVersionStr)

            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = packageInfo.versionName ?: "0"

            if (VersionUtils.isNewerVersion(latestVersion, currentVersion)) {
                sendNotification(latestVersion)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendNotification(version: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "update_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // KEY POINT: Pass a flag to tell UI to open About Screen
            putExtra("NAVIGATE_TO_ABOUT", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Update Available")
            .setContentText("Version $version is ready to download.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}