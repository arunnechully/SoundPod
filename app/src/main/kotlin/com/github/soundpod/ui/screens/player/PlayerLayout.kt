package com.github.soundpod.ui.screens.player

import androidx.compose.runtime.Composable
import com.github.soundpod.enums.PlayerLayout
import com.github.soundpod.utils.playerlayout
import com.github.soundpod.utils.rememberPreference

@Composable
fun PlayerLayout(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onBack: () -> Unit
) {
    val playerLayoutState = rememberPreference(playerlayout, PlayerLayout.Default)
    val currentLayout = playerLayoutState.value

    when (currentLayout) {
        PlayerLayout.Default -> {
            MainPlayerContent(
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onBack = onBack
            )
        }
        PlayerLayout.New -> {
            NewPlayer(
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist,
                onBack = onBack
            )
        }
    }
}