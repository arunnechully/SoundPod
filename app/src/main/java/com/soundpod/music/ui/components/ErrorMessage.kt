package com.soundpod.music.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorMessage(
    onAddFolderClick: () -> Unit // 👈 callback for button action
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No songs found or permission denied.",
            color = textColor,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAddFolderClick
        ) {
            Text(
                text = "Add a Folder",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
