package com.github.soundpod.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

fun downloadApk(context: Context, url: String): Long {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Downloading Update")
        .setDescription("Downloading latest SoundPod APK…")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            "SoundPod-latest.apk"
        )

    val manager = context.getSystemService(DownloadManager::class.java)
    return manager.enqueue(request)
}
