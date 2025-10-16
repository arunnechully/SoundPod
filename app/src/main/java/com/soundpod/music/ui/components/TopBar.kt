package com.soundpod.music.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onSearch: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val iconColor = if (isDarkTheme) Color.White else Color.Black
    val containerColor = if (isDarkTheme) Color.Black else Color(0xFFF6F6F8)

    TopAppBar(
        title = {
            Text(
                text = "SoundPod",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
        },
        actions = {
            IconButton(
                onClick = onSearch
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = iconColor
                )
            }
            IconButton(
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = iconColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = if (isDarkTheme) Color.White else Color.Black
        )
    )
}
