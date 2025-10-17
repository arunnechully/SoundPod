package com.soundpod.music.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorMessage() {
    val context = LocalContext.current

    // Load bitmap from assets
    val bitmap = context.assets.open("img/a1.webp").use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }
    val isDarkTheme = isSystemInDarkTheme()
    val text = if (isDarkTheme) Color.White else Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image from assets
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "No songs found",
            modifier = Modifier.size(250.dp) // adjust size as needed
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No songs found or permission denied.",
            color = text,
            fontSize = 18.sp
        )
    }
}
