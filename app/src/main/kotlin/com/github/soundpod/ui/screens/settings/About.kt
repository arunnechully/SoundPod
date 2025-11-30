package com.github.soundpod.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.github.api.GitHub
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.common.autoCheckEnabled
import com.github.soundpod.ui.common.setAutoCheckEnabled
import com.github.soundpod.ui.common.setShowUpdateAlert
import com.github.soundpod.ui.common.showUpdateAlert
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.VersionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Define UI States for the Update Card
sealed class UpdateStatus {
    object Checking : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Available(val version: String, val downloadUrl: String, val size: Long) : UpdateStatus()
    data class Downloading(val progress: Float) : UpdateStatus()
    data class ReadyToInstall(val file: File) : UpdateStatus()
    object Error : UpdateStatus()
}

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

    // State
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Checking) }
    var autoCheckEnabled by rememberSaveable { mutableStateOf(true) }
    var showAlertEnabled by rememberSaveable { mutableStateOf(true) }
    var settingsLoaded by remember { mutableStateOf(false) }

    BackHandler(onBack = onBackClick)

    // Load Settings
    LaunchedEffect(Unit) {
        launch {
            autoCheckEnabled(context).collect {
                autoCheckEnabled = it
                settingsLoaded = true
            }
        }
        launch {
            showUpdateAlert(context).collect {
                showAlertEnabled = it
                settingsLoaded = true
            }
        }
        // Auto-check for updates when screen opens
        launch(Dispatchers.IO) {
            checkForUpdates(currentVersion) { status ->
                updateStatus = status
            }
        }
    }

    SettingsScreenLayout(
        title = stringResource(id = R.string.about),
        onBackClick = onBackClick,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                // App Icon
                Icon(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(125.dp)
                        .aspectRatio(1f),
                    tint = MaterialTheme.colorScheme.primary
                )

                // Version Text
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "v$currentVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- Update Status Section (Replaces the Check Button) ---
                SettingsCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = updateStatus,
                            label = "UpdateStatus"
                        ) { status ->
                            when (status) {
                                is UpdateStatus.Checking -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(stringResource(id = R.string.checking_for_updates))
                                    }
                                }

                                is UpdateStatus.UpToDate -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("You are on the latest version")
                                    }
                                }

                                is UpdateStatus.Available -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(id = R.string.new_version_available) + ": ${status.version}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    downloadAndInstall(
                                                        context,
                                                        status.downloadUrl,
                                                        onProgress = { progress ->
                                                            updateStatus = UpdateStatus.Downloading(progress)
                                                        },
                                                        onFinished = { file ->
                                                            updateStatus = UpdateStatus.ReadyToInstall(file)
                                                            installApk(context, file)
                                                        },
                                                        onError = {
                                                            updateStatus = UpdateStatus.Error
                                                        }
                                                    )
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.CloudDownload, null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Download & Update")
                                        }
                                    }
                                }

                                is UpdateStatus.Downloading -> {
                                    val progressAnimated by animateFloatAsState(targetValue = status.progress, label = "progress")
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Downloading... ${(status.progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { progressAnimated },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                        )
                                    }
                                }

                                is UpdateStatus.ReadyToInstall -> {
                                    Button(
                                        onClick = { installApk(context, status.file) }
                                    ) {
                                        Icon(Icons.Default.SystemUpdate, null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Install Update")
                                    }
                                }

                                is UpdateStatus.Error -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Error checking/downloading update")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.spacer + 8.dp))

                // ... Rest of your existing links ...
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
                    if (settingsLoaded) {
                        SwitchSetting(
                            icon = IconSource.Vector(Icons.Default.Update),
                            title = stringResource(id = R.string.autocheck),
                            description = stringResource(id = R.string.autocheck_description),
                            switchState = autoCheckEnabled,
                            onSwitchChange = { enabled ->
                                autoCheckEnabled = enabled
                                CoroutineScope(Dispatchers.IO).launch {
                                    setAutoCheckEnabled(context, enabled)
                                }
                            }
                        )

                        SwitchSetting(
                            icon = IconSource.Vector(Icons.Default.Notifications),
                            title = stringResource(id = R.string.update_alert),
                            description = stringResource(id = R.string.update_alert_desription),
                            switchState = showAlertEnabled,
                            onSwitchChange = { enabled ->
                                showAlertEnabled = enabled
                                CoroutineScope(Dispatchers.IO).launch {
                                    setShowUpdateAlert(context, enabled)
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

// --- Helper Logic ---

suspend fun checkForUpdates(currentVersion: String, onResult: (UpdateStatus) -> Unit) {
    try {
        val release = GitHub.getLastestRelease()
        val latestVersion = release?.name?.let { VersionUtils.extractVersion(it) }
        val apkAsset = release?.assets?.firstOrNull { it.name.endsWith(".apk") }

        if (latestVersion != null && apkAsset != null) {
            val isNew = VersionUtils.isNewerVersion(latestVersion, currentVersion)
            if (isNew) {
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

fun downloadAndInstall(
    context: Context,
    urlString: String,
    onProgress: (Float) -> Unit,
    onFinished: (File) -> Unit,
    onError: () -> Unit
) {
    try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            onError()
            return
        }

        val fileLength = connection.contentLength
        val file = File(context.externalCacheDir, "update.apk")
        if (file.exists()) file.delete()

        val input = connection.inputStream
        val output = file.outputStream()

        val data = ByteArray(4096)
        var total: Long = 0
        var count: Int

        while (input.read(data).also { count = it } != -1) {
            total += count.toLong()
            if (fileLength > 0) {
                onProgress(total.toFloat() / fileLength)
            }
            output.write(data, 0, count)
        }

        output.flush()
        output.close()
        input.close()

        onFinished(file)

    } catch (e: Exception) {
        e.printStackTrace()
        onError()
    }
}

fun installApk(context: Context, file: File) {
    // Check for Android 8.0+ permission to install packages
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            // Send user to settings to allow permission
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }
    }

    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Ensure you have this provider in AndroidManifest
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback or show error toast
    }
}