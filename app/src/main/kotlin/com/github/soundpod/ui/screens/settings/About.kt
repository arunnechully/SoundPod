package com.github.soundpod.ui.screens.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.common.UpdateStatus
import com.github.soundpod.ui.common.seamlessUpdateEnabled
import com.github.soundpod.ui.common.setSeamlessUpdateEnabled
import com.github.soundpod.ui.common.setShowUpdateAlert
import com.github.soundpod.ui.common.showUpdateAlert
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.github.UpdateMessage
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.ui.github.checkForUpdates
import com.github.soundpod.ui.github.downloadAndInstall
import com.github.soundpod.ui.github.installApkInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
    var showAlertEnabled by rememberSaveable { mutableStateOf(true) }
    val status = updateStatus
    BackHandler(onBack = onBackClick)

    // Re-check permission on Resume
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasPermission = context.packageManager.canRequestPackageInstalls()

            if (showPermissionDialog && hasPermission) {
                showPermissionDialog = false
                seamlessUpdateEnabled = true
                scope.launch(Dispatchers.IO) {
                    setSeamlessUpdateEnabled(context, true)
                    checkForUpdates(context, currentVersion, true) { updateStatus = it }
                }
            }

            if (seamlessUpdateEnabled && !hasPermission) {
                seamlessUpdateEnabled = false
                scope.launch(Dispatchers.IO) {
                    setSeamlessUpdateEnabled(context, false)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        launch { showUpdateAlert(context).collect { showAlertEnabled = it } }
        launch {
            seamlessUpdateEnabled(context).collect { savedValue ->
                seamlessUpdateEnabled = savedValue
                checkForUpdates(context, currentVersion, savedValue) { updateStatus = it }
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
                Icon(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(125.dp)
                        .aspectRatio(1f),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
                if (status is UpdateStatus.Available) {
                    Text(
                        text = stringResource(id = R.string.new_version_available),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else Text(
                    text = "Version $currentVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                UpdateMessage(
                    status = updateStatus,
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
                            delay(1500)
                            installApkInternal(context, file)
                        }
                    },
                )

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

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCard {
                    SwitchSetting(
                        icon = IconSource.Vector(Icons.Default.Update),
                        title = stringResource(id = R.string.seamless_update),
                        description = stringResource(id = R.string.seamless_update_description),
                        switchState = seamlessUpdateEnabled,
                        onSwitchChange = { isChecked ->
                            if (isChecked) {
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

                    SwitchSetting(
                        icon = IconSource.Vector(Icons.Default.Notifications),
                        title = stringResource(id = R.string.update_alert),
                        description = stringResource(id = R.string.update_alert_desription),
                        switchState = showAlertEnabled,
                        onSwitchChange = { enabled ->
                            showAlertEnabled = enabled
                            CoroutineScope(Dispatchers.IO).launch {
                                setShowUpdateAlert(
                                    context,
                                    enabled
                                )
                            }
                        }
                    )
                }
            }
        }
    )

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = { Icon(Icons.Outlined.Security, null) },
            title = { Text(stringResource(id = R.string.enable_seamless_update)) },
            text = { Text(stringResource(id = R.string.enable_seamless_update_description)) },
            confirmButton = {
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                        context.startActivity(intent)
                    }
                }) { Text(stringResource(id = R.string.settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        )
    }
}