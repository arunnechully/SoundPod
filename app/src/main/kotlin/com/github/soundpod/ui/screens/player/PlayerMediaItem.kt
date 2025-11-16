package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.utils.DisposableListener

@Composable
fun PlayerMediaItem(
    onGoToArtist: (() -> Unit)?,
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    var currentItem by remember {
        mutableStateOf(player.currentMediaItem, neverEqualPolicy())
    }

    // Update when player changes
    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentItem = mediaItem
            }
        }
    }

    val mediaItem = currentItem ?: return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {

        // TITLE
        Text(
            text = mediaItem.mediaMetadata.title?.toString().orEmpty(),
            modifier = Modifier.basicMarquee(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ARTIST
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    enabled = onGoToArtist != null,
                    onClick = onGoToArtist ?: {}
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)   // bigger hitbox
        ) {
            Text(
                text = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
}
