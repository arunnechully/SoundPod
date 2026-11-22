package com.github.soundpod.ui.screens.player.layout

import androidx.compose.runtime.Composable
import com.github.soundpod.ui.screens.player.layout.main.ApplePlayer
import com.github.soundpod.utils.PlayerLayout
import com.github.soundpod.ui.screens.player.layout.main.MainPlayerContent
import com.github.soundpod.ui.screens.player.layout.main.SamsungPlayer
import com.github.soundpod.ui.screens.player.layout.main.YoutubePlayer
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
    onAddToPlaylist: () -> Unit = {},
    showLyrics: Boolean,
    onTogglePlaylist: (Boolean) -> Unit
) {
    val playerLayoutState = rememberPreference(playerlayout, PlayerLayout.SamsungMusic)
    val currentLayout = playerLayoutState.value

    when (playerLayoutState.value) {
        PlayerLayout.SamsungMusic -> {
            SamsungPlayer(
                expandProgress = expandProgress,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onTrackDetailsClick = onGoToTrackDetails,
                onAddToPlaylist = onAddToPlaylist,
                onBack = onBack,
                showPlaylist = showPlaylist,
                onLyricsClick = onLyricsClick,
                onSettingsClick = onSettingsClick,
                onSleepTimerClick = onSleepTimerClick,
                showLyrics = showLyrics,
                onTogglePlaylist = onTogglePlaylist

            )
        }

        PlayerLayout.AppleMusic -> {
            ApplePlayer(
                expandProgress = expandProgress,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onTrackDetailsClick = onGoToTrackDetails,
                onAddToPlaylist = onAddToPlaylist,
                onBack = onBack,
                showPlaylist = showPlaylist,
                onLyricsClick = onLyricsClick,
                onSettingsClick = onSettingsClick,
                onSleepTimerClick = onSleepTimerClick,
                showLyrics = showLyrics,
                onTogglePlaylist = onTogglePlaylist,
                layoutMode = currentLayout
            )
        }

        PlayerLayout.YoutubeMusic -> {
            YoutubePlayer(
                expandProgress = expandProgress,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onTrackDetailsClick = onGoToTrackDetails,
                onAddToPlaylist = onAddToPlaylist,
                onBack = onBack,
                showPlaylist = showPlaylist,
                onLyricsClick = onLyricsClick,
                onSettingsClick = onSettingsClick,
                onSleepTimerClick = onSleepTimerClick,
                showLyrics = showLyrics,
                onTogglePlaylist = onTogglePlaylist,
                layoutMode = currentLayout
            )
        }

        PlayerLayout.SoundPod -> {
            MainPlayerContent(
                expandProgress = expandProgress,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onTrackDetailsClick = onGoToTrackDetails,
                onAddToPlaylist = onAddToPlaylist,
                onBack = onBack,
                showPlaylist = showPlaylist,
                onLyricsClick = onLyricsClick,
                onSettingsClick = onSettingsClick,
                onSleepTimerClick = onSleepTimerClick,
                showLyrics = showLyrics,
                onTogglePlaylist = onTogglePlaylist,
            )
        }
    }
}
