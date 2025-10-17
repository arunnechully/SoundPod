package com.soundpod.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soundpod.music.ui.components.HorizontalTabs
import com.soundpod.music.ui.components.TopBar

@Composable
fun HomeScreen(
    navController: NavController
) {
    val isDarkTheme = isSystemInDarkTheme()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val background = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    // 1. State to control the visibility of the DropdownMenu
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopBar(
            onSearch = { navController.navigate("search") },
            // 2. Set the state to true when the 'More' button is clicked
            onMore = { showMenu = true }
        )

        // 3. DropdownMenu is placed here, right after the TopBar
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false } // Closes the menu when clicking outside or pressing back
        ) {
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    showMenu = false
                    // TODO: Handle navigation or action for Settings
                    navController.navigate("settings")
                }
            )
            DropdownMenuItem(
                text = { Text("About") },
                onClick = {
                    showMenu = false
                    // TODO: Handle navigation or action for About
                    navController.navigate("about")
                }
            )
            DropdownMenuItem(
                text = { Text("Logout") },
                onClick = {
                    showMenu = false
                    // TODO: Handle Logout action
                }
            )
        }

        Spacer(modifier = Modifier.padding(vertical = 2.dp))

        HorizontalTabs(pagerState = pagerState)

        // ... (rest of the code for HorizontalPager remains the same)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .background(color = background)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> Tracks()
                    1 -> Box(Modifier.fillMaxSize()) { Text("Albums") }
                    2 -> Box(Modifier.fillMaxSize()) { Text("Artists") }
                }
            }
        }
    }
}