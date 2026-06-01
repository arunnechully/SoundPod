package com.github.soundpod.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.github.soundpod.BuildConfig
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource

@Suppress("KotlinConstantConditions")
@Composable
fun PrivacySettingsContent() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true

    val isStorageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    val isInstallUnknownGranted = if (BuildConfig.ENABLE_UPDATER) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    } else false

    Column {
        SettingsGroup(
            title = stringResource(id = R.string.permissions)
        ) {
            if (BuildConfig.ENABLE_UPDATER) {
                SettingsColumn(
                    icon = IconSource.Vector(Icons.Outlined.Notifications),
                    title = stringResource(id = R.string.notifications),
                    description = if (isNotificationGranted) stringResource(id = R.string.allowed) else stringResource(
                        id = R.string.denied
                    ),
                    onClick = { openAppSettings(context) },
                )
            }
            SettingsColumn(
                icon = IconSource.Vector(Icons.Outlined.MusicNote),
                title = stringResource(id = R.string.audio_permission),
                description = if (isStorageGranted) stringResource(id = R.string.allowed) else stringResource(
                    id = R.string.denied
                ),
                onClick = { openAppSettings(context) },
            )
            if (BuildConfig.ENABLE_UPDATER) {
                SettingsColumn(
                    icon = IconSource.Vector(Icons.Outlined.Security),
                    title = stringResource(id = R.string.install_unknown_apps),
                    description = if (isInstallUnknownGranted) stringResource(id = R.string.allowed) else stringResource(
                        id = R.string.denied
                    ),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val intent =
                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                            context.startActivity(intent)
                        }
                    },
                )
            }
        }
        SettingsGroup(
            title = stringResource(id = R.string.storage)
        ) {
            SettingsColumn(
                icon = IconSource.Vector(Icons.Outlined.Folder),
                title = stringResource(id = R.string.local_data),
                description = stringResource(id = R.string.local_data_discription),
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
