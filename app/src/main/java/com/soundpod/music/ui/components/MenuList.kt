package com.soundpod.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun HorizontalMenu() {
    val tabs = listOf("Home", "Library", "Artists", "Playlists", "Genres")

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column {
        // Tab Row
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 8.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = {}
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                val backgroundColor =
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else Color.Transparent

                Tab(
                    selected = isSelected,
                    onClick = {
                        selectedTabIndex = index
                        // Scroll pager to the selected tab
                        coroutineScope.launch { pagerState.scrollToPage(index) }
                    },
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor),
                    text = {
                        Text(
                            text = title,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Pager (Assuming TabsPager is defined elsewhere and handles the content)
        TabsPager(
            tabs = tabs,
            pagerState = pagerState
        )

        // Sync selected tab with pager page
        LaunchedEffect(pagerState.currentPage) {
            selectedTabIndex = pagerState.currentPage
        }
    }
}