package com.github.soundpod.ui.screens.builtinplaylist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.ui.components.PlaylistScreenLayout

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun BuiltInPlaylistScreen(
    builtInPlaylist: BuiltInPlaylist,
    pop: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    BackHandler(enabled = true) {
        pop()
    }

    PlaylistScreenLayout(
        title = {
            Text(
                text = when (builtInPlaylist) {
                    BuiltInPlaylist.Favorites -> stringResource(id = R.string.favorites)
                    BuiltInPlaylist.Offline -> stringResource(id = R.string.offline)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorPalette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        onBackClick = pop,
        headerContent = {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorPalette.text.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = when (builtInPlaylist) {
                        BuiltInPlaylist.Favorites -> painterResource(id = R.drawable.heart)
                        BuiltInPlaylist.Offline -> painterResource(id = R.drawable.offline)
                    },
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colorPalette.text.copy(alpha = 0.4f)
                )
            }
        },
        content = {
            BuiltInPlaylistSongs(
                builtInPlaylist = builtInPlaylist,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist
            )
        }
    )
}
