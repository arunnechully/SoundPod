package com.github.soundpod.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.core.ui.LocalAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onSearch: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    TopAppBar(
        title = {
            Text(
                text = "SoundPod",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text
            )
        },
        actions = {
            OutlinedButton(
                onClick = onSearch,
                shape = RoundedCornerShape(60), // pill shape
                border = BorderStroke(1.dp, Color.Gray),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colorPalette.text
                )

                Text(
                    text = "Search",
                    color = colorPalette.text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedIconButton(
                onClick = onSettingsClick,
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = colorPalette.text
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = colorPalette.text
        )
    )
}