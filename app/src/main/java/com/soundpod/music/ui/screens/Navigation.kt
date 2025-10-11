@file:OptIn(ExperimentalAnimationApi::class)

package com.soundpod.music.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController()
) {

    val isDarkTheme = isSystemInDarkTheme()

    NavHost(
        navController = navController,
        startDestination = "home/main",
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDarkTheme) Color.Black else Color(0xFFF6F6F8) // Off-white
            ),
        enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
    ) {
        composable("home/main") {
            HomeScreen(
                navController = navController
            )
        }
    }
}