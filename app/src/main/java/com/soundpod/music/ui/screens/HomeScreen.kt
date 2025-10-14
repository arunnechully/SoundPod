package com.soundpod.music.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soundpod.music.ui.components.TopBar
import com.soundpod.music.ui.components.TransparentHorizontalMenu

@Composable
fun HomeScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopBar()
        Spacer(
            modifier = Modifier
                .padding(vertical = 2.dp)
        )
        TransparentHorizontalMenu()
    }
}