package com.soundpod.music.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.soundpod.music.ui.components.SearchBar

@Composable
fun SearchScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SearchBar(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}