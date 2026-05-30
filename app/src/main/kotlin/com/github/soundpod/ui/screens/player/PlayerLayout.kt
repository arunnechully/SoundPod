package com.github.soundpod.ui.screens.player

import androidx.compose.runtime.Composable
import com.github.soundpod.enums.PlayerLayout
import com.github.soundpod.utils.playerlayout
import com.github.soundpod.utils.rememberPreference

@Composable
fun PlayerLayout(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToTrackDetails: (String) -> Unit,
    onBack: () -> Unit,
    showPlaylist: Boolean,
    onLyricsClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    showLyrics: Boolean,
    onTogglePlaylist: (Boolean) -> Unit
) {
    val playerLayoutState = rememberPreference(playerlayout, PlayerLayout.Default)
    val currentLayout = playerLayoutState.value

    MainPlayerContent(
        layoutMode = currentLayout,
        onGoToAlbum = onGoToAlbum,
        onGoToArtist = onGoToArtist,
//        onGoToTrackDetails = onGoToTrackDetails,
        onBack = onBack,
        showPlaylist = showPlaylist,
        onLyricsClick = onLyricsClick,
//        onSettingsClick = onSettingsClick,
        showLyrics = showLyrics,
        onTogglePlaylist = onTogglePlaylist

    )
}