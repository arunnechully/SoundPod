package com.github.soundpod.ui.screens.settings

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.github.api.GitHub
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.common.UpdateStatus
import com.github.soundpod.ui.common.autoCheckEnabled
import com.github.soundpod.ui.common.seamlessUpdateEnabled
import com.github.soundpod.ui.common.setAutoCheckEnabled
import com.github.soundpod.ui.common.setSeamlessUpdateEnabled
import com.github.soundpod.ui.common.setShowUpdateAlert
import com.github.soundpod.ui.common.showUpdateAlert
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.VersionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettings(
    onBackClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentVersion = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Checking) }
    var seamlessUpdateEnabled by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    var autoCheckEnabled by rememberSaveable { mutableStateOf(true) }
    var showAlertEnabled by rememberSaveable { mutableStateOf(true) }
    var settingsLoaded by remember { mutableStateOf(false) }

    BackHandler(onBack = onBackClick)

    // Re-check permission on Resume (if user went to settings and came back)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (seamlessUpdateEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                seamlessUpdateEnabled = false // Permission was revoked
            }
        }
    }

    // Load Settings & Check for Updates
    LaunchedEffect(Unit) {
        launch { autoCheckEnabled(context).collect { autoCheckEnabled = it; settingsLoaded = true } }
        launch { showUpdateAlert(context).collect { showAlertEnabled = it; settingsLoaded = true } }
        launch(Dispatchers.IO) {
            checkForUpdates(currentVersion) { updateStatus = it }
        }
        launch {
            seamlessUpdateEnabled(context).collect { savedValue ->
                seamlessUpdateEnabled = savedValue
            }
        }    }

    SettingsScreenLayout(
        title = stringResource(id = R.string.about),
        onBackClick = onBackClick,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterHorizontally).width(125.dp).aspectRatio(1f),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "v$currentVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- 2. UPDATE STATUS CARD (Play Store Style) ---
                SettingsCard {
                    PlayStoreUpdateCard(
                        status = updateStatus,
                        currentVersion = currentVersion,
                        onUpdateClick = { downloadUrl ->
                            scope.launch(Dispatchers.IO) {
                                downloadAndInstall(
                                    context = context,
                                    urlString = downloadUrl,
                                    isSeamless = seamlessUpdateEnabled,
                                    onProgress = { updateStatus = UpdateStatus.Downloading(it) },
                                    onFinished = { file ->
                                        updateStatus = if (seamlessUpdateEnabled) {
                                            UpdateStatus.ReadyToInstall(file)
                                        } else {
                                            UpdateStatus.DownloadedToPublic(file)
                                        }
                                    },
                                    onError = { updateStatus = UpdateStatus.Error }
                                )
                            }
                        },
                        onInstallClick = { file ->
                            updateStatus = UpdateStatus.Installing
                            scope.launch {
                                delay(1500) // Fake delay to show spinner
                                installApkInternal(context, file)
                            }
                        },
                        onOpenFolderClick = { file ->
                            openPublicFile(context, file)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.spacer + 8.dp))

                SettingsCard {
                    SettingColum(
                        icon = IconSource.Icon(painterResource(id = R.drawable.github)),
                        title = stringResource(id = R.string.source_code),
                        onClick = { uriHandler.openUri("https://github.com/arunnechully/SoundPod") },
                    )
                    SettingColum(
                        icon = IconSource.Icon(painterResource(id = R.drawable.idea)),
                        title = stringResource(id = R.string.suggest_an_idea),
                        onClick = { uriHandler.openUri("https://github.com/arunnechully/SoundPod/issues/new?template=feature_request.md") },
                    )
                    SettingColum(
                        icon = IconSource.Icon(painterResource(id = R.drawable.bug)),
                        title = stringResource(id = R.string.report_a_bug),
                        onClick = { uriHandler.openUri("https://github.com/arunnechully/SoundPod/issues/new?template=bug_report.md") },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCard {
                    SeamlessUpdateToggle(
                        checked = seamlessUpdateEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                // Check permission BEFORE enabling
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                                    showPermissionDialog = true
                                } else {
                                    seamlessUpdateEnabled = true
                                    CoroutineScope(Dispatchers.IO).launch {
                                        setSeamlessUpdateEnabled(context, true)
                                    }
                                }
                            } else {
                                seamlessUpdateEnabled = false
                                CoroutineScope(Dispatchers.IO).launch {
                                    setSeamlessUpdateEnabled(context, false)
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCard {
                    if (settingsLoaded) {
                        SwitchSetting(
                            icon = IconSource.Vector(Icons.Default.Update),
                            title = stringResource(id = R.string.autocheck),
                            description = stringResource(id = R.string.autocheck_description),
                            switchState = autoCheckEnabled,
                            onSwitchChange = { enabled ->
                                autoCheckEnabled = enabled
                                CoroutineScope(Dispatchers.IO).launch { setAutoCheckEnabled(context, enabled) }
                            }
                        )
                        SwitchSetting(
                            icon = IconSource.Vector(Icons.Default.Notifications),
                            title = stringResource(id = R.string.update_alert),
                            description = stringResource(id = R.string.update_alert_desription),
                            switchState = showAlertEnabled,
                            onSwitchChange = { enabled ->
                                showAlertEnabled = enabled
                                CoroutineScope(Dispatchers.IO).launch { setShowUpdateAlert(context, enabled) }
                            }
                        )
                    }
                }
            }
        }
    )

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = { Icon(Icons.Outlined.Security, null) },
            title = { Text("Enable Seamless Updates?") },
            text = { Text("To install updates internally, SoundPod needs permission to install unknown apps.\n\nOtherwise, updates will be saved to your Downloads folder.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                }) { Text("Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SeamlessUpdateToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SwitchSetting(
        icon = IconSource.Vector(if (checked) Icons.Default.SystemUpdate else Icons.Default.Folder),
        title = "Seamless Updates",
        description = if (checked) "Download & Install directly." else "Save to Downloads folder.",
        switchState = checked,
        onSwitchChange = onCheckedChange
    )
}

@Composable
fun PlayStoreUpdateCard(
    status: UpdateStatus,
    currentVersion: String,
    onUpdateClick: (String) -> Unit,
    onInstallClick: (File) -> Unit,
    onOpenFolderClick: (File) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("App Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            val statusText = when (status) {
                is UpdateStatus.Checking -> "Checking..."
                is UpdateStatus.UpToDate -> "Version $currentVersion is latest"
                is UpdateStatus.Available -> "Version ${status.version} available"
                is UpdateStatus.Downloading -> "Downloading..."
                is UpdateStatus.Installing -> "Installing..."
                is UpdateStatus.ReadyToInstall -> "Ready to install"
                is UpdateStatus.DownloadedToPublic -> "Saved to Downloads"
                is UpdateStatus.Error -> "Update failed"
            }
            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(contentAlignment = Alignment.Center) {
            when (status) {
                is UpdateStatus.Checking -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                is UpdateStatus.UpToDate -> {}
                is UpdateStatus.Available -> {
                    Button(onClick = { onUpdateClick(status.downloadUrl) }, modifier = Modifier.height(36.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Update")
                    }
                }
                is UpdateStatus.Downloading -> {
                    val progressAnimated by animateFloatAsState(targetValue = status.progress, label = "progress")
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        CircularProgressIndicator(progress = { progressAnimated }, modifier = Modifier.size(48.dp))
                        Text("${(status.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UpdateStatus.ReadyToInstall -> {
                    Button(onClick = { onInstallClick(status.file) }) { Text("Install") }
                }
                is UpdateStatus.DownloadedToPublic -> {
                    Button(onClick = { onOpenFolderClick(status.file) }) {
                        Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open")
                    }
                }
                is UpdateStatus.Installing -> CircularProgressIndicator(modifier = Modifier.size(48.dp))
                is UpdateStatus.Error -> Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

suspend fun checkForUpdates(currentVersion: String, onResult: (UpdateStatus) -> Unit) {
    try {
        val release = GitHub.getLastestRelease()
        val latestVersion = release?.name?.let { VersionUtils.extractVersion(it) }
        val apkAsset = release?.assets?.firstOrNull { it.name.endsWith(".apk") }

        if (latestVersion != null && apkAsset != null) {
            if (VersionUtils.isNewerVersion(latestVersion, currentVersion)) {
                onResult(UpdateStatus.Available(latestVersion, apkAsset.browserDownloadUrl, apkAsset.size))
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
            if (connection.responseCode != HttpURLConnection.HTTP_OK) { onError(); return }

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
        } catch (e: Exception) { e.printStackTrace(); onError() }
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
        val request = DownloadManager.Request(Uri.parse(urlString))
            .setTitle("SoundPod Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SoundPod-Update.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        var downloading = true
        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

                if (bytesTotal > 0) onProgress(bytesDownloaded.toFloat() / bytesTotal.toFloat())

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false
                    val publicFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SoundPod-Update.apk")
                    onFinished(publicFile)
                } else if (status == DownloadManager.STATUS_FAILED) {
                    downloading = false
                    onError()
                }
            }
            cursor.close()
            delay(500)
        }
    } catch (e: Exception) { e.printStackTrace(); onError() }
}

// --- ACTION 1: SEAMLESS INSTALLER ---
fun installApkInternal(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) { e.printStackTrace() }
}

// --- ACTION 2: EXTERNAL OPENER ---
fun openPublicFile(context: Context, file: File) {
    try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            Toast.makeText(context, "Saved to Downloads: SoundPod-Update.apk", Toast.LENGTH_LONG).show()
        }
    }
}