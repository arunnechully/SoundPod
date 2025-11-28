package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.github.api.Asset
import com.github.api.GitHub
import com.github.api.formatFileSize
import com.github.soundpod.LocalPlayerPadding
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
import com.github.soundpod.utils.downloadApk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettings(
    onBackClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val playerPadding = LocalPlayerPadding.current

    var isShowingDialog by remember { mutableStateOf(false) }
    var latestVersion by rememberSaveable { mutableStateOf<String?>(null) }
    var newVersionAvailable by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var apkUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val currentVersion =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

    var autoCheckEnabled by rememberSaveable { mutableStateOf(true) }
    var showAlertEnabled by rememberSaveable { mutableStateOf(true) }

    var settingsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        autoCheckEnabled(context).collect {
            autoCheckEnabled = it
            settingsLoaded = true
        }
    }

    LaunchedEffect(Unit) {
        showUpdateAlert(context).collect {
            showAlertEnabled = it
            settingsLoaded = true
        }
    }



    SettingsScreenLayout(
        title = stringResource(id = R.string.about),
        onBackClick = onBackClick,
        content = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = stringResource(id = R.string.app_name),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(125.dp)
                        .aspectRatio(1f),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "${stringResource(id = R.string.app_name)} v$currentVersion",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        isShowingDialog = true
                        latestVersion = null
                        newVersionAvailable = null
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Update,
                        contentDescription = stringResource(id = R.string.check_for_updates)
                    )

                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

                    Text(text = stringResource(id = R.string.check_for_updates))
                }

                Spacer(modifier = Modifier.height(Dimensions.spacer + 8.dp))

                SettingsCard {
                    SettingColum(
                        icon = IconSource.Icon(painterResource(id = R.drawable.github)),
                        title = stringResource(id = R.string.source_code),
                        onClick = {
                            uriHandler.openUri("https://github.com/arunnechully/SoundPod")
                        },
                    )
                    SettingColum(
                        icon = IconSource.Icon(painterResource(id = R.drawable.idea)),
                        title = stringResource(id = R.string.suggest_an_idea),
                        onClick = {
                            uriHandler.openUri(
                                "https://github.com/arunnechully/SoundPod/issues/new?template=feature_request.md"
                            )
                        },
                    )

                    SettingColum(
                        icon = IconSource.Icon(painterResource(id = R.drawable.bug)),
                        title = stringResource(id = R.string.report_a_bug),
                        onClick = {
                            uriHandler.openUri(
                                "https://github.com/arunnechully/SoundPod/issues/new?template=bug_report.md"
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCard {
                    if (settingsLoaded) {
                        SwitchSetting(
                            icon = Icons.Default.Update,
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
                            icon = Icons.Default.Notifications,
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

            if (isShowingDialog) {

                var apkAsset by rememberSaveable { mutableStateOf<Asset?>(null) }


                LaunchedEffect(Unit) {
                    val release = GitHub.getLastestRelease()
                    apkAsset = release?.assets?.firstOrNull { it.name.endsWith(".apk") }
                    apkUrl = apkAsset?.browserDownloadUrl

                    latestVersion = release?.name?.let { VersionUtils.extractVersion(it) }
                    newVersionAvailable = VersionUtils.isNewerVersion(latestVersion ?: "0", currentVersion)
                }




                AlertDialog(
                    onDismissRequest = { isShowingDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = { isShowingDialog = false }
                        ) {
                            Text(text = stringResource(id = R.string.close))
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(
                                id = when (newVersionAvailable) {
                                    true -> R.string.new_version_available
                                    false -> R.string.no_updates_available
                                    else -> R.string.checking_for_updates
                                }
                            )
                        )
                    },
                    text = {
                        when (newVersionAvailable) {
                            null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            true -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    apkAsset?.let { asset ->
                                        Text(
                                            text = "File Size: ${formatFileSize(asset.size)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }



                                    Text(
                                        text = stringResource(
                                            id = R.string.version,
                                            latestVersion ?: ""
                                        ),
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    FilledTonalButton(
                                        onClick = {
                                            apkUrl?.let { url ->
                                                downloadApk(context, url)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.download),
                                            contentDescription = "Update Now"
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(text = "Update Now")
                                    }
                                }
                            }

                            false -> {}
                        }
                    }
                )
            }
        }
    )
}
