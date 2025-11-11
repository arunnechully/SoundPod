package com.github.soundpod.ui.screens.settings.newsettings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.soundpod.Database
import com.github.soundpod.R
import com.github.soundpod.internal
import com.github.soundpod.query
import com.github.soundpod.service.PlayerService
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.screens.settings.SettingsInformation
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
fun Backup(
    onBackClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    val context = LocalContext.current

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

    SettingsScreenLayout(
        title = stringResource(id = R.string.backup_restore),
        onBackClick = onBackClick,
        content = {

            SettingsCard {
                SettingColum(
                    textColor = textColor,
                    painterRes = R.drawable.backup,
                    title = stringResource(id = R.string.backup),
                    description = stringResource(id = R.string.backup_description),
                    onClick = {
                        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

                        try {
                            backupLauncher.launch("soundpod${dateFormat.format(Date())}.db")
                        } catch (_: ActivityNotFoundException) {
                            context.toast("Couldn't find an application to create documents")
                        }
                    }
                )

                SettingColum(
                    textColor = textColor,
                    icon = Icons.Default.Restore,
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
                    }
                )
            }

            SettingsInformation(text = stringResource(id = R.string.restore_information))
        }
    )
}