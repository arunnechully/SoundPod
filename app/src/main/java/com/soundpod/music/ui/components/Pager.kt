package com.soundpod.music.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
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
import com.soundpod.music.utils.RequestAudioPermission

@Composable
fun TabsPager(tabs: List<String>, pagerState: PagerState) {

    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    RequestAudioPermission {
        songs = MusicRepository.getAllSongs(context)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (songs.isEmpty()) {
                Text(
                    text = "No songs found or permission denied."
                )
            } else {
                LazyColumn {
                    items(songs) { song ->
                        SongItem(song, context)
                    }
                }
            }
        }
    }
}

