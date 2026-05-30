@file:Suppress("KotlinConstantConditions")

package com.github.soundpod.ui.screens.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.core.ui.LocalAppearance
import com.github.soundpod.BuildConfig
import com.github.soundpod.R
import com.github.soundpod.github.UpdateMessage
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.common.UpdateStatus
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.viewmodels.AboutViewModel

@Suppress("unused")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettings(
    onBackClick: () -> Unit,
    viewModel: AboutViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // Collect States from ViewModel
    val updateStatus by viewModel.updateStatus.collectAsState()
    val seamlessUpdateEnabled by viewModel.seamlessUpdateEnabled.collectAsState()
    val showAlertEnabled by viewModel.showAlertEnabled.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()

    BackHandler(onBack = onBackClick)

    val checkInstallPermission = {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else true
        } catch (e: SecurityException) {
            false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onResume(checkInstallPermission())
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

                if (updateStatus is UpdateStatus.Available) {
                    Text(
                        text = stringResource(id = R.string.new_version_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Version ${viewModel.currentVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                if (BuildConfig.ENABLE_UPDATER) {
                    UpdateMessage(
                        status = updateStatus,
                        onUpdateClick = { viewModel.downloadUpdate() },
                        onInstallClick = { file -> viewModel.installUpdate(file) }
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.spacer + 8.dp))

                SettingsCard {
                    SettingColumn(
                        icon = IconSource.Icon(painterResource(id = R.drawable.github)),
                        title = stringResource(id = R.string.source_code),
                        onClick = { uriHandler.openUri("https://github.com/arunnechully/SoundPod") },
                    )
                    SettingColumn(
                        icon = IconSource.Icon(painterResource(id = R.drawable.idea)),
                        title = stringResource(id = R.string.suggest_an_idea),
                        onClick = { uriHandler.openUri("https://github.com/arunnechully/SoundPod/issues/new") },
                    )
                    SettingColumn(
                        icon = IconSource.Icon(painterResource(id = R.drawable.bug)),
                        title = stringResource(id = R.string.report_a_bug),
                        onClick = { uriHandler.openUri("https://github.com/arunnechully/SoundPod/issues/new") },
                    )
                }

                val (colorPalette) = LocalAppearance.current

                if (!BuildConfig.ENABLE_UPDATER) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(id = R.string.distribution_info),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorPalette.text.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsCard {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.f_droid),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(id = R.string.f_droid_build),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = stringResource(id = R.string.updates_handled_by_fdroid),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                            )
                            Button(
                                onClick = { uriHandler.openUri("https://f-droid.org/packages/com.github.soundpod") },
                                modifier = Modifier.fillMaxWidth(0.5f),
                                shape = MaterialTheme.shapes.large,
                            ) {
                                Text(text = stringResource(id = R.string.view_on_f_droid))
                            }
                        }
                    }
                }

                if (BuildConfig.ENABLE_UPDATER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsCard {
                        SwitchSetting(
                            icon = IconSource.Vector(Icons.Default.Update),
                            title = stringResource(id = R.string.seamless_update),
                            description = stringResource(id = R.string.seamless_update_description),
                            switchState = seamlessUpdateEnabled,
                            onSwitchChange = { isChecked ->
                                viewModel.toggleSeamlessUpdate(isChecked, checkInstallPermission())
                            }
                        )

                        SwitchSetting(
                            icon = IconSource.Vector(Icons.Default.Notifications),
                            title = stringResource(id = R.string.update_alert),
                            description = stringResource(id = R.string.update_alert_desription),
                            switchState = showAlertEnabled,
                            onSwitchChange = { enabled ->
                                viewModel.toggleUpdateAlert(enabled)
                            }
                        )
                    }
                }
            }
        }
    )

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionDialog() },
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
                TextButton(onClick = { viewModel.dismissPermissionDialog() }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}