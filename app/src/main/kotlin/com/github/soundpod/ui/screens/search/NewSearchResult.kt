package com.github.soundpod.ui.screens.search

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout

@Composable
fun NewSearchResult(
    navController: NavController,
    resultType: String?
) {
    SettingsScreenLayout(
        title = resultType ?: "Results",
        onBackClick = { navController.popBackStack() }
    ) {
        SettingsCard{
            //todo
        }
    }
}