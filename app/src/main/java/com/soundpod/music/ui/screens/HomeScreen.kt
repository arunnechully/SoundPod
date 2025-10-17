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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopBar(
            onSearch = { navController.navigate("search") },
            onSettingsClick = { navController.navigate("settings") }
        )

        Spacer(modifier = Modifier.padding(vertical = 2.dp))

        HorizontalTabs(pagerState = pagerState)

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