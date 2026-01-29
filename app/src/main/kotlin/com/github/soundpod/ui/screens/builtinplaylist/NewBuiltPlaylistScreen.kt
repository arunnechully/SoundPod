package com.github.soundpod.ui.screens.builtinplaylist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.soundpod.R
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun NewBuiltInPlaylistScreen(
    builtInPlaylist: BuiltInPlaylist,
    pop: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    BackHandler(enabled = true) {
        pop()
    }

    SettingsScreenLayout(
        title = {
            Text(
                text = when (builtInPlaylist) {
                    BuiltInPlaylist.Favorites -> stringResource(id = R.string.favorites)
                    BuiltInPlaylist.Offline -> stringResource(id = R.string.offline)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        scrollable = false,
        horizontalPadding = 0.dp,
        onBackClick = pop,
    ) {
        SettingsCard {
            NewBuiltInPlaylistSongs(
                builtInPlaylist = builtInPlaylist,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist
            )
        }
    }
}