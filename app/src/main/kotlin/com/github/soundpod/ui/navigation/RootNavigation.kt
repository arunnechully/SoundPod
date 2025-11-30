package com.github.soundpod.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.soundpod.ui.screens.settings.AboutSettings
import com.github.soundpod.ui.screens.settings.AppearanceSettings
import com.github.soundpod.ui.screens.settings.BackupSettings
import com.github.soundpod.ui.screens.settings.CacheSettings
import com.github.soundpod.ui.screens.settings.ExperimentSettings
import com.github.soundpod.ui.screens.settings.MoreSettings
import com.github.soundpod.ui.screens.settings.PlayerSettings
import com.github.soundpod.ui.screens.settings.PrivacySettings
import com.github.soundpod.ui.screens.settings.SettingsScreen

@Composable
fun RootNavigation(
    mainActivityIntent: Intent?
) {
    val rootNavController = rememberNavController()

    LaunchedEffect(Unit) {
        if (mainActivityIntent?.getBooleanExtra("NAVIGATE_TO_ABOUT", false) == true) {
            rootNavController.navigate(Routes.About)
            mainActivityIntent.removeExtra("NAVIGATE_TO_ABOUT")
        }
    }

    NavHost(
        navController = rootNavController,
        startDestination = "music_main",
        enterTransition = { Transitions.enter() },
        exitTransition = { Transitions.exit() },
        popEnterTransition = { Transitions.popEnter() },
        popExitTransition = { Transitions.popExit() }
    ) {
        composable("music_main") {
            MainWrapper(
                onNavigateToSettings = {
                    rootNavController.navigate(Routes.Settings)
                }
            )
        }

        // Settings (Slides OVER the player)
        composable(route = Routes.Settings::class) {
            SettingsScreen(
                navController = rootNavController,
                onBackClick = { rootNavController.popBackStack() }
            )
        }

        composable(route = Routes.Appearance::class) {
            AppearanceSettings(onBackClick = { rootNavController.popBackStack() })
        }
        composable(route = Routes.Player::class) {
            PlayerSettings(onBackClick = { rootNavController.popBackStack() })
        }
        composable(route = Routes.Privacy::class) {
            PrivacySettings(onBackClick = { rootNavController.popBackStack() })
        }
        composable(route = Routes.Backup::class) {
            BackupSettings(onBackClick = { rootNavController.popBackStack() })
        }
        composable(route = Routes.Storage::class) {
            CacheSettings(onBackClick = { rootNavController.popBackStack() })
        }
        composable(route = Routes.More::class) {
            MoreSettings(onBackClick = { rootNavController.popBackStack() })
        }
        composable(route = Routes.Experiment::class) {
            ExperimentSettings(onBackClick = { rootNavController.popBackStack() })
        }
        composable(route = Routes.About::class) {
            AboutSettings(onBackClick = { rootNavController.popBackStack() })
        }
    }
}