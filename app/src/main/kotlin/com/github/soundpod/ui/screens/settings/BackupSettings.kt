package com.github.soundpod.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.AutoBackUp
import com.github.soundpod.internal
import com.github.soundpod.query
import com.github.soundpod.service.PlayerService
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.autoBackup
import com.github.soundpod.utils.autoBackupUriPrefKey
import com.github.soundpod.utils.intent
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.toast
import com.github.soundpod.worker.scheduleAutoBackup
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.system.exitProcess

@Suppress("SpellCheckingInspection")
@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettings(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    var autoBackup by rememberPreference(autoBackup, AutoBackUp.OFF)
    var autoBackupUriString by rememberPreference(autoBackupUriPrefKey, "")

    val autoBackupUri = remember(autoBackupUriString) {
        if (autoBackupUriString.isNotEmpty()) autoBackupUriString.toUri() else null
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)

            autoBackupUriString = uri.toString()
            scheduleAutoBackup(context, autoBackup)

            context.toast("Backup location saved")
        }
    }

    BackHandler(onBack = onBackClick)

    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.sqlite3")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            query {
                db.checkpoint()

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
                db.checkpoint()
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

    BackHandler(onBack = onBackClick)

    SettingsScreenLayout(
        title = stringResource(id = R.string.backup_restore),
        onBackClick = onBackClick,
        content = {
            Spacer(modifier = Modifier.height(Dimensions.spacer))
            Text(
                text = stringResource(id = R.string.localbackup),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingColumn(
                    icon = IconSource.Icon(painterResource(id = R.drawable.local_backup)),
                    title = stringResource(id = R.string.backup),
                    description = stringResource(id = R.string.backup_description),
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
                        val formattedDate = LocalDateTime.now().format(formatter)

                        try {
                            backupLauncher.launch("SoundPod_Manual_$formattedDate.db")
                        } catch (_: ActivityNotFoundException) {
                            context.toast("Couldn't find an application to create documents")
                        }
                    }
                )

                SettingColumn(
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
            Spacer(modifier = Modifier.height(16.dp))
            SettingsCard {
                SwitchSetting(
                    icon = IconSource.Vector(Icons.Default.Autorenew),
                    title = stringResource(id = R.string.auto_backup),
                    description = stringResource(id = R.string.auto_backup_description),
                    switchState = autoBackup != AutoBackUp.OFF,
                    onSwitchChange = { isChecked ->
                        val newFrequency = if (isChecked) AutoBackUp.DAILY else AutoBackUp.OFF
                        autoBackup = newFrequency
                        scheduleAutoBackup(context, newFrequency)
                    },
                )
                AnimatedVisibility(
                    visible = autoBackup != AutoBackUp.OFF,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        SettingColumn(
                            icon = IconSource.Vector(Icons.Default.Folder),
                            title = stringResource(id = R.string.backup_location),
                            description = autoBackupUri?.lastPathSegment ?: stringResource(R.string.backup_location_description),
                            onClick = { directoryPickerLauncher.launch(null) }
                        )
                        EnumValueSelectorSettingsEntry(
                            title = stringResource(id = R.string.auto_backup),
                            selectedValue = autoBackup,
                            onValueSelected = { newFrequency ->
                                autoBackup = newFrequency
                                scheduleAutoBackup(context, newFrequency)
                            },
                            icon = IconSource.Vector(Icons.Default.DateRange),
                            valueText = { stringResource(it.resourceId) }
                        )
                    }
                }
            }

            SettingsInformation(text = stringResource(id = R.string.restore_information))
        }
    )
}