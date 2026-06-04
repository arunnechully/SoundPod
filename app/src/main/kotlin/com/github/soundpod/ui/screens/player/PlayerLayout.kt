package com.github.soundpod.ui.screens.player

import androidx.compose.runtime.Composable
import com.github.soundpod.enums.PlayerLayout
import com.github.soundpod.utils.playerlayout
import com.github.soundpod.utils.rememberPreference

@Composable
fun PlayerLayout(
    expandProgress: Float,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToTrackDetails: () -> Unit,
    onBack: () -> Unit,
    showPlaylist: Boolean,
    onLyricsClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onSleepTimerClick: () -> Unit = {},
    showLyrics: Boolean,
    onTogglePlaylist: (Boolean) -> Unit
) {
    val playerLayoutState = rememberPreference(playerlayout, PlayerLayout.Default)
    val currentLayout = playerLayoutState.value

    MainPlayerContent(
        expandProgress = expandProgress,
        layoutMode = currentLayout,
        onGoToAlbum = onGoToAlbum,
        onGoToArtist = onGoToArtist,
        onTrackDetailsClick = onGoToTrackDetails,
        onBack = onBack,
        showPlaylist = showPlaylist,
        onLyricsClick = onLyricsClick,
        onSettingsClick = onSettingsClick,
        onSleepTimerClick = onSleepTimerClick,
        showLyrics = showLyrics,
        onTogglePlaylist = onTogglePlaylist

    )
}
