package com.github.soundpod.ui.screens.settings.newsettings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Battery0Bar
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.soundpod.R
import com.github.soundpod.service.PlayerMediaBrowserService
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.ui.screens.settings.SettingsEntry
import com.github.soundpod.ui.screens.settings.SettingsInformation
import com.github.soundpod.ui.screens.settings.SwitchSettingEntry
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.isAtLeastAndroid12
import com.github.soundpod.utils.isAtLeastAndroid13
import com.github.soundpod.utils.isAtLeastAndroid6
import com.github.soundpod.utils.isIgnoringBatteryOptimizations
import com.github.soundpod.utils.isInvincibilityEnabledKey
import com.github.soundpod.utils.isShowingThumbnailInLockscreenKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.toast

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSettings(
    onBackClick: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    val context = LocalContext.current

    var isAndroidAutoEnabled by remember {
        val component = ComponentName(context, PlayerMediaBrowserService::class.java)
        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        mutableStateOf(
            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
            policy = object : SnapshotMutationPolicy<Boolean> {
                override fun equivalent(a: Boolean, b: Boolean): Boolean {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        if (b) enabledFlag else disabledFlag,
                        PackageManager.DONT_KILL_APP
                    )
                    return a == b
                }
            }
        )
    }

    var showInfoDialog by remember { mutableStateOf(false) }

    var isInvincibilityEnabled by rememberPreference(isInvincibilityEnabledKey, false)
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(context.isIgnoringBatteryOptimizations) }
    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations
        }
    var isShowingThumbnailInLockscreen by rememberPreference(
        isShowingThumbnailInLockscreenKey,
        false
    )


    SettingsScreenLayout(
        title =stringResource(id = R.string.more_settings),
        onBackClick = onBackClick,
        content = {

            SettingsCard {

                if (!isAtLeastAndroid13) {
                    SwitchSettingEntry(
                        title = stringResource(id = R.string.show_song_cover),
                        text = stringResource(id = R.string.show_song_cover_description),
                        icon = Icons.Outlined.Image,
                        isChecked = isShowingThumbnailInLockscreen,
                        onCheckedChange = { isShowingThumbnailInLockscreen = it }
                    )
                }

                SwitchSetting(
                    textColor = textColor,
                    painterRes = R.drawable.android_auto,
                    title = stringResource(id = R.string.android_auto),
                    description = stringResource(id = R.string.android_auto_description),
                    switchState = isAndroidAutoEnabled,
                    onSwitchChange = { enabled ->
                        isAndroidAutoEnabled = enabled
                        if (enabled) {
                            showInfoDialog = true      // Only show when switching ON
                        }
                    }
                )

                /* Dialog */
                if (showInfoDialog) {
                    AlertDialog(
                        onDismissRequest = { showInfoDialog = false },
                        title = {
                            Text(text = stringResource(id = R.string.android_auto))
                        },
                        text = {
                            SettingsInformation(
                                text = stringResource(id = R.string.android_auto_information)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showInfoDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimensions.spacer))

            Text(
                text = stringResource(id = R.string.service_lifetime),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )

            SettingsEntry(
                title = stringResource(id = R.string.ignore_battery_optimizations),
                text = if (isIgnoringBatteryOptimizations) {
                    stringResource(id = R.string.already_unrestricted)
                } else {
                    stringResource(id = R.string.disable_background_restrictions)
                },
                icon = Icons.Outlined.Battery0Bar,
                onClick = {
                    if (!isAtLeastAndroid6) return@SettingsEntry

                    try {
                        activityResultLauncher.launch(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                        )
                    } catch (_: ActivityNotFoundException) {
                        try {
                            activityResultLauncher.launch(
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                        } catch (_: ActivityNotFoundException) {
                            context.toast("Couldn't find battery optimization settings, please whitelist Music You manually")
                        }
                    }
                },
                isEnabled = !isIgnoringBatteryOptimizations
            )

            SwitchSettingEntry(
                title = stringResource(id = R.string.service_lifetime),
                text = stringResource(id = R.string.service_lifetime_description),
                icon = Icons.Outlined.Stars,
                isChecked = isInvincibilityEnabled,
                onCheckedChange = { isInvincibilityEnabled = it }
            )

            SettingsInformation(
                text = stringResource(id = R.string.service_lifetime_information) +
                        if (isAtLeastAndroid12) "\n" + stringResource(id = R.string.service_lifetime_information_plus) else ""
            )

            if (isAtLeastAndroid13) {
                val intent = Intent(
                    Settings.ACTION_APP_LOCALE_SETTINGS,
                    "package:${context.packageName}".toUri()
                )

                SettingsEntry(
                    title = stringResource(id = R.string.app_language),
                    text = stringResource(id = R.string.configure_app_language),
                    icon = Icons.Outlined.Language,
                    onClick = {
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            context.toast("Couldn't find app language settings, please configure them manually")
                        }
                    }
                )
            }


            if (isAtLeastAndroid12) {
                val intent = Intent(
                    Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                    "package:${context.packageName}".toUri()
                )

                SettingsEntry(
                    title = stringResource(id = R.string.open_supported_links_by_default),
                    text = stringResource(id = R.string.configure_supported_links),
                    icon = Icons.Outlined.AddLink,
                    onClick = {
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            context.toast("Couldn't find supported links settings, please configure them manually")
                        }
                    }
                )
            }

        }
    )
}