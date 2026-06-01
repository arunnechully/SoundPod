package com.github.soundpod

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.soundpod.enums.AppThemeColor
import com.github.soundpod.service.PlayerService
import com.github.soundpod.ui.navigation.SettingsDestinations
import com.github.soundpod.ui.screens.settings.SettingsScreen
import com.github.soundpod.ui.styling.AppTheme
import com.github.soundpod.utils.appTheme
import com.github.soundpod.utils.rememberPreference

class SettingsActivity : ComponentActivity() {

    private var binder by mutableStateOf<PlayerService.Binder?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlayerService.Binder) {
                this@SettingsActivity.binder = service
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, PlayerService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                window.isNavigationBarContrastEnforced = false
            }
        } else {
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.BLACK
        }

        val initialScreenId = intent.getStringExtra("SCREEN_ID") ?: SettingsDestinations.MAIN

        setContent {
            val appTheme by rememberPreference(appTheme, AppThemeColor.System)
            var currentScreenId by remember { mutableStateOf(initialScreenId) }
            val screenStack = remember { mutableStateListOf(initialScreenId) }

            val onBack: () -> Unit = {
                if (screenStack.size > 1) {
                    screenStack.removeAt(screenStack.size - 1)
                    currentScreenId = screenStack.last()
                } else {
                    finish()
                }
            }

            BackHandler(onBack = onBack)

            val darkTheme = when (appTheme) {
                AppThemeColor.System -> isSystemInDarkTheme()
                AppThemeColor.Dark -> true
                AppThemeColor.Light -> false
            }

            androidx.compose.runtime.LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { darkTheme }
                )
            }

            AppTheme(
                darkTheme = darkTheme,
                usePureBlack = false,
                useMaterialNeutral = false,
            ) {
                CompositionLocalProvider(value = LocalPlayerServiceBinder provides binder) {
                    SettingsScreen(
                        screenId = currentScreenId,
                        onBackClick = onBack,
                        onOptionClick = { routeId ->
                            screenStack.add(routeId)
                            currentScreenId = routeId
                        }
                    )
                }
            }
        }
    }
}
