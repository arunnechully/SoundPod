package com.github.soundpod.ui.github

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.github.soundpod.ui.common.UpdateStatus

@Composable
fun DownloadProgressBar(
    status: UpdateStatus.Downloading
){

    val progressAnimated by animateFloatAsState(
        targetValue = status.progress,
        label = "DownloadProgress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(40.dp)
            .width(120.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .drawBehind {
                drawRect(
                    color = primaryColor,
                    size = Size(
                        width = size.width * progressAnimated,
                        height = size.height
                    )
                )
            }
    ) {
        Text(
            text = "${(status.progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}