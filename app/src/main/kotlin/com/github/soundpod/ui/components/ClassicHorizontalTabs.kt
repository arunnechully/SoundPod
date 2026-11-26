package com.github.soundpod.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class TabItem(
    @StringRes val title: Int? = null,
    @DrawableRes val icon: Int? = null,
)

@Composable
fun ClassicHorizontalTabs(
    tabs: List<TabItem>,
    pagerState: PagerState? = null,
    selectedTabIndex: Int? = null,
    onTabSelected: ((Int) -> Unit)? = null,
    selectedBackgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    unselectedBackgroundColor: Color = Color.Transparent,
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = Color.Transparent,
) {
    val coroutineScope = rememberCoroutineScope()
    var internalSelectedIndex by remember { mutableIntStateOf(0) }

    val currentTabIndex = pagerState?.currentPage
        ?: selectedTabIndex
        ?: internalSelectedIndex

    Column {
        PrimaryScrollableTabRow(
            selectedTabIndex = currentTabIndex,
            edgePadding = 8.dp,
            containerColor = containerColor,
            divider = {},
            indicator = {}
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = currentTabIndex == index
                val contentColor = if (isSelected) selectedContentColor else unselectedContentColor
                val backgroundColor = if (isSelected) selectedBackgroundColor else unselectedBackgroundColor

                Tab(
                    selected = isSelected,
                    onClick = {
                        if (pagerState != null) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        } else {
                            internalSelectedIndex = index
                        }
                        onTabSelected?.invoke(index)
                    },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(backgroundColor)
                        .height(34.dp),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tab.icon?.let { iconRes ->
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = tab.title?.let { stringResource(it) },
                                    modifier = Modifier.size(18.dp),
                                    tint = contentColor
                                )
                            }
                            tab.title?.let { titleRes ->
                                Text(
                                    text = stringResource(id = titleRes),
                                    color = contentColor
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}