package com.soundpod.music.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.soundpod.music.MediaControllerHolder
import com.soundpod.music.data.Song

@Composable
fun SongItem(song: Song, context: Context) {

    val mediaController = MediaControllerHolder.controller
    val isDarkTheme = isSystemInDarkTheme()
    val iconColor = if (isDarkTheme) Color.White else Color.Black
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                mediaController?.let { controller ->
                    val mediaItem = MediaItem.Builder()
                        .setUri(song.uri)
                        .setMediaId(song.id.toString())
                        .setTag(song)
                        .build()

                    controller.setMediaItem(mediaItem)
                    controller.prepare()
                    controller.play()
                }
            }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleMedium,
            color = iconColor
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = iconColor
        )
    }
}
