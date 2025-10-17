package com.soundpod.music.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.soundpod.music.MediaControllerHolder
import com.soundpod.music.data.Song

@Composable
fun SongItem(
    song: Song,
    songs: List<Song>,
    context: Context
) {
    val mediaController = MediaControllerHolder.controller
    val isDarkTheme = isSystemInDarkTheme()
    var currentMediaId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaController) {
        mediaController?.addListener(
            object : androidx.media3.common.Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentMediaId = mediaItem?.mediaId
                }
            }
        )
        currentMediaId = mediaController?.currentMediaItem?.mediaId
    }

    val isPlaying = currentMediaId == song.id.toString()

    val titleColor =
        if (isPlaying) MaterialTheme.colorScheme.primary
        else if (isDarkTheme) Color.White
        else Color.Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                mediaController?.let { controller ->
                    val mediaItems = songs.map { s ->
                        MediaItem.Builder()
                            .setUri(s.uri)
                            .setMediaId(s.id.toString())
                            .setTag(s)
                            .build()
                    }
                    val selectedIndex = songs.indexOf(song)
                    controller.setMediaItems(mediaItems, selectedIndex, 0)
                    controller.prepare()
                    controller.play()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkTheme) Color.Gray else Color.DarkGray
            )
        }
    }
}
