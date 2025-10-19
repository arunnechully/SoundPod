@file:OptIn(ExperimentalAnimationApi::class)

package com.soundpod.music.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soundpod.music.ui.screens.SearchScreen
import com.soundpod.music.ui.screens.home.HomeScreen
import com.soundpod.music.ui.screens.settings.About
import com.soundpod.music.ui.screens.settings.Appearance
import com.soundpod.music.ui.screens.settings.Backup
import com.soundpod.music.ui.screens.settings.Experiment
import com.soundpod.music.ui.screens.settings.MoreSettings
import com.soundpod.music.ui.screens.settings.Player
import com.soundpod.music.ui.screens.settings.Privacy
import com.soundpod.music.ui.screens.settings.SettingsScreen
import com.soundpod.music.ui.screens.settings.Storage

sealed class Routes(val route: String) {
    object Home : Routes("home/main")
    object Search : Routes("search")

    // Settings
    object Settings : Routes("settings")
    object Appearance : Routes("appearance")
    object Player : Routes("player")
    object Privacy : Routes("privacy")
    object Backup : Routes("backup")
    object Storage : Routes("storage")
    object More : Routes("more")
    object Experiment : Routes("experiment")
    object About : Routes("about")
}

// Reusable transitions for cleaner code
@OptIn(ExperimentalAnimationApi::class)
private fun defaultEnterTransition() =
    slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn()

@OptIn(ExperimentalAnimationApi::class)
private fun defaultExitTransition() =
    slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut()

@OptIn(ExperimentalAnimationApi::class)
private fun defaultPopEnterTransition() =
    slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn()

@OptIn(ExperimentalAnimationApi::class)
private fun defaultPopExitTransition() =
    slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut()

@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController()
) {
    val isDarkTheme = isSystemInDarkTheme()

    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = Modifier
            .fillMaxSize()
            .background(color = if (isDarkTheme) Color.Black else Color(0xFFF6F6F8)),
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {

        // Home
        composable(Routes.Home.route) {
            HomeScreen(navController = navController)
        }

        // Search
        composable(Routes.Search.route) {
            SearchScreen(navController = navController)
        }

        // Settings & sub-screens
        composable(Routes.Settings.route) {
            SettingsScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.Appearance.route) {
            Appearance(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.Player.route) {
            Player(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.Privacy.route) {
            Privacy(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.Backup.route) {
            Backup(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.Storage.route) {
            Storage(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.More.route) {
            MoreSettings(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.Experiment.route) {
            Experiment(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.About.route) {
            About(onBackClick = { navController.popBackStack() })
        }
    }
}
