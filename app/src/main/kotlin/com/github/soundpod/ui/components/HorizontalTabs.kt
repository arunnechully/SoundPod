package com.github.soundpod.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun HorizontalTabs(
    pagerState: PagerState
) {
    val tabs = listOf("Tracks", "Playlist", "Favorite", "Albums", "Artists", "Genres", "Downloads") // Added more tabs to better demonstrate centering
    val coroutineScope = rememberCoroutineScope()

    // The selectedTabIndex is directly tied to the PagerState.currentPage
    val selectedTabIndex = pagerState.currentPage

    Column {
        PrimaryScrollableTabRow(
            // Use the PagerState's current page as the selected index.
            // PrimaryScrollableTabRow automatically tries to center the selected tab.
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
                        // Scroll the Pager when the tab is clicked
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                        // NOTE: You no longer need to manually update a 'selectedTabIndex' state here
                        // because it's sourced from 'pagerState.currentPage'.
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(backgroundColor)
                        .height(32.dp),
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
    }
}