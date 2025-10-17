package com.soundpod.music.ui.screens

import com.soundpod.music.ui.components.ErrorMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.soundpod.music.data.MusicRepository
import com.soundpod.music.data.Song
import com.soundpod.music.ui.components.SongItem
import com.soundpod.music.utils.RequestAudioPermission

@Composable
fun Tracks() {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    RequestAudioPermission {
        songs = MusicRepository.getAllSongs(context)
    }

    if (songs.isEmpty()) {
        ErrorMessage() // ✅ replaces Text
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs) { song ->
                SongItem(
                    song = song,
                    songs = songs
                )
            }
        }
    }

}
