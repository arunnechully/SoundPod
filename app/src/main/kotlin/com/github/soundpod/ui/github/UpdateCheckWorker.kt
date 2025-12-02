package com.github.soundpod.ui.github

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.api.GitHub
import com.github.soundpod.R
import com.github.soundpod.SettingsActivity
import com.github.soundpod.ui.common.autoCheckEnabled
import com.github.soundpod.ui.common.showUpdateAlert
import com.github.soundpod.ui.navigation.SettingsDestinations
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return Result.failure()
            }
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
        val channelId = "update_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when a new update is available"
            }
            notificationManager.createNotificationChannel(channel)
        }
        val intent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SCREEN_ID", SettingsDestinations.ABOUT)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle("Update Available")
            .setContentText("Version $version is ready to download.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {

            NotificationManagerCompat.from(context).notify(1001, notification)
        }
    }
}