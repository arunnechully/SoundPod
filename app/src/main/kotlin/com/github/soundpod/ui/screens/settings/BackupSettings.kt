@file:Suppress("AssignedValueIsNeverRead")

package com.github.soundpod.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.Database
import com.github.soundpod.R
import com.github.soundpod.internal
import com.github.soundpod.query
import com.github.soundpod.service.PlayerService
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.intent
import com.github.soundpod.utils.toast
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettings(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.sqlite3")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                Database.checkpoint()

                context.applicationContext.contentResolver.openOutputStream(uri)
                    ?.use { outputStream ->
                        FileInputStream(internal.path).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                Database.checkpoint()
                internal.close()

                context.applicationContext.contentResolver.openInputStream(uri)
                    ?.use { inputStream ->
                        FileOutputStream(internal.path).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                context.stopService(context.intent<PlayerService>())
                exitProcess(0)
            }
        }
// TODO: Migrate this to DataStore or SharedPreferences for permanent storage
    var isAutoBackupEnabled by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBackClick)

    SettingsScreenLayout(
        title = stringResource(id = R.string.backup_restore),
        onBackClick = onBackClick,
        content = {
            Spacer(modifier = Modifier.height(Dimensions.spacer))
            Text(
                text = stringResource(id = R.string.cloud_backup),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingColum(
                    icon = IconSource.Icon(painterResource(id = R.drawable.cloud_backup)),
                    title = stringResource(id = R.string.sync_to_cloud),
                    description = "Upload your database to Google Drive",
                    onClick = {
                        // TODO: Implement Google Drive Upload logic
                        context.toast("Cloud Sync coming soon")
                    },
                )

                SettingColum(
                    icon = IconSource.Icon(painterResource(id = R.drawable.cloud_restore)),
                    title = stringResource(id = R.string.cloud_restore),
                    description = "Download and overwrite current database",
                    onClick = {
                        // TODO: Implement Google Drive Download logic
                        context.toast("Cloud Restore coming soon")
                    },
                )

                SwitchSetting(
                    icon = IconSource.Vector(Icons.Outlined.CloudSync),
                    title = "Auto Cloud Backup",
                    description = "Automatically sync database changes to the cloud",
                    switchState = isAutoBackupEnabled,
                    onSwitchChange = { isAutoBackupEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.spacer))
            Text(
                text = stringResource(id = R.string.localbackup),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingColum(
                    icon = IconSource.Icon(painterResource(id = R.drawable.local_backup)),
                    title = stringResource(id = R.string.backup),
                    description = stringResource(id = R.string.backup_description),
                    onClick = {
                        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

                        try {
                            backupLauncher.launch("soundpod${dateFormat.format(Date())}.db")
                        } catch (_: ActivityNotFoundException) {
                            context.toast("Couldn't find an application to create documents")
                        }
                    },
                )

                SettingColum(
                    icon = IconSource.Vector(Icons.Default.Restore),
                    title = stringResource(id = R.string.restore),
                    description = stringResource(id = R.string.restore_description),
                    onClick = {
                        try {
                            restoreLauncher.launch(
                                arrayOf(
                                    "application/vnd.sqlite3",
                                    "application/x-sqlite3",
                                    "application/octet-stream"
                                )
                            )
                        } catch (_: ActivityNotFoundException) {
                            context.toast("Couldn't find an application to open documents")
                        }
                    },
                )
            }

            SettingsInformation(text = stringResource(id = R.string.restore_information))
        }
    )
}