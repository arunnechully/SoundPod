package com.soundpod.music.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soundpod.music.ui.components.ErrorMessage
import com.soundpod.music.ui.components.SongItem
import com.soundpod.music.utils.RequestAudioPermission
import com.soundpod.music.viewmodel.SongViewModel

@Composable
fun Tracks(viewModel: SongViewModel = viewModel()) {

    val songs by viewModel.songs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    RequestAudioPermission {
        // Load songs only once when permission is granted
        LaunchedEffect(Unit) {
            viewModel.loadSongs()
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        songs.isEmpty() -> {
            ErrorMessage(
                onAddFolderClick = {
                    // TODO: open folder picker
                }
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(songs) { song ->
                    SongItem(
                        song = song,
                        songs = songs,
                        context = LocalContext.current
                    )
                }
            }
        }
    }
}
