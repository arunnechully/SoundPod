package com.github.soundpod

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import com.github.soundpod.enums.AppThemeColor
import com.github.soundpod.ui.navigation.SettingsDestinations
import com.github.soundpod.ui.screens.settings.*
import com.github.soundpod.ui.styling.AppTheme
import com.github.soundpod.utils.appTheme
import com.github.soundpod.utils.rememberPreference

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val screenId = intent.getStringExtra("SCREEN_ID") ?: SettingsDestinations.MAIN

        setContent {

            val appTheme by rememberPreference(appTheme, AppThemeColor.System)

            val darkTheme = when (appTheme) {
                AppThemeColor.System -> isSystemInDarkTheme()
                AppThemeColor.Dark -> true
                AppThemeColor.Light -> false
            }

            AppTheme(
                darkTheme = darkTheme,
                usePureBlack = false,
                useMaterialNeutral = false,
            ) {
                when (screenId) {
                    SettingsDestinations.MAIN -> {
                        SettingsScreen(
                            onBackClick = { finish() },
                            onOptionClick = { routeId -> launchSubScreen(routeId) }
                        )
                    }
                    SettingsDestinations.APPEARANCE -> AppearanceSettings(onBackClick = { finish() })
                    SettingsDestinations.PLAYER -> PlayerSettings(onBackClick = { finish() })
                    SettingsDestinations.PRIVACY -> PrivacySettings(onBackClick = { finish() })
                    SettingsDestinations.BACKUP -> BackupSettings(onBackClick = { finish() })
                    SettingsDestinations.DATABASE -> CacheSettings(onBackClick = { finish() })
                    SettingsDestinations.MORE -> MoreSettings(onBackClick = { finish() })
                    SettingsDestinations.EXPERIMENT -> ExperimentSettings(onBackClick = { finish() })
                    SettingsDestinations.ABOUT -> AboutSettings(onBackClick = { finish() })
                }
            }
        }
    }
    private fun launchSubScreen(screenId: String) {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            putExtra("SCREEN_ID", screenId)
        }
        startActivity(intent)
        // Note: Do NOT call finish() here. We want to stack the new one on top.
    }
}