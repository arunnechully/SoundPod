package com.github.soundpod.utils

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.github.api.GitHub
import com.github.soundpod.ui.common.UpdateStatus
import kotlinx.coroutines.delay
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

fun downloadApk(context: Context, url: String): Long {
    val request = DownloadManager.Request(url.toUri())
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
suspend fun checkForUpdates(currentVersion: String, onResult: (UpdateStatus) -> Unit) {
    try {
        val release = GitHub.getLastestRelease()
        val latestVersion = release?.name?.let { VersionUtils.extractVersion(it) }
        val apkAsset = release?.assets?.firstOrNull { it.name.endsWith(".apk") }

        if (latestVersion != null && apkAsset != null) {
            if (VersionUtils.isNewerVersion(latestVersion, currentVersion)) {
                onResult(
                    UpdateStatus.Available(
                        latestVersion,
                        apkAsset.browserDownloadUrl,
                        apkAsset.size
                    )
                )
            } else {
                onResult(UpdateStatus.UpToDate)
            }
        } else {
            onResult(UpdateStatus.Error)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(UpdateStatus.Error)
    }
}

suspend fun downloadAndInstall(
    context: Context,
    urlString: String,
    isSeamless: Boolean,
    onProgress: (Float) -> Unit,
    onFinished: (File) -> Unit,
    onError: () -> Unit
) {
    if (isSeamless) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                onError(); return
            }

            val file = File(context.externalCacheDir, "update.apk")
            if (file.exists()) file.delete()

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = file.outputStream()
            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                total += count.toLong()
                if (fileLength > 0) onProgress(total.toFloat() / fileLength)
                output.write(data, 0, count)
            }
            output.flush(); output.close(); input.close()
            onFinished(file)
        } catch (e: Exception) {
            e.printStackTrace(); onError()
        }
    } else {
        downloadViaDownloadManager(context, urlString, onProgress, onFinished, onError)
    }
}

suspend fun downloadViaDownloadManager(
    context: Context,
    urlString: String,
    onProgress: (Float) -> Unit,
    onFinished: (File) -> Unit,
    onError: () -> Unit
) {
    try {
        val request = DownloadManager.Request(urlString.toUri())
            .setTitle("SoundPod Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "SoundPod-Update.apk"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        var downloading = true
        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val idxBytes =
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val idxTotal = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val idxStatus = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)

                val bytesDownloaded = cursor.getInt(idxBytes)
                val bytesTotal = cursor.getInt(idxTotal)
                val status = cursor.getInt(idxStatus)

                if (bytesTotal > 0) onProgress(bytesDownloaded.toFloat() / bytesTotal.toFloat())

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false
                    val publicFile = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "SoundPod-Update.apk"
                    )
                    onFinished(publicFile)
                } else if (status == DownloadManager.STATUS_FAILED) {
                    downloading = false
                    onError()
                }
            }
            cursor.close()
            delay(500)
        }
    } catch (e: Exception) {
        e.printStackTrace(); onError()
    }
}

fun installApkInternal(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun openPublicFile(context: Context, file: File) {
    try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Fallback
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Saved to Downloads: SoundPod-Update.apk", Toast.LENGTH_LONG)
                .show()
        }
    }
}