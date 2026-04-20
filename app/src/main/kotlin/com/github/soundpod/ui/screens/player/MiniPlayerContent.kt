package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.shouldBePlaying

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayerContent(
    openPlayer: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    var shouldBePlaying by remember { mutableStateOf(player.shouldBePlaying) }

    var nullableMediaItem by remember {
        mutableStateOf(player.currentMediaItem, neverEqualPolicy())
    }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = player.shouldBePlaying
            }
        }
    }

    val mediaItem = nullableMediaItem ?: return

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                interactionSource = remember {MutableInteractionSource() },
                indication = null,
                onClick = openPlayer
            )
    ) {
        ListItem(
            headlineContent = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = mediaItem.mediaMetadata.title?.toString() ?: "",
                        modifier = Modifier.basicMarquee(),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            },
            supportingContent = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                        modifier = Modifier.basicMarquee(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = {
                Spacer(modifier = Modifier.size(43.dp))
            },
            trailingContent = {
                MiniPlayerControl(
                    playing = shouldBePlaying,
                    onClick = {
                        if (shouldBePlaying) {
                            player.pause()
                        } else {
                            when (player.playbackState) {
                                Player.STATE_IDLE -> player.prepare()
                                Player.STATE_ENDED -> player.seekToDefaultPosition(0)
                                Player.STATE_BUFFERING,
                                Player.STATE_READY -> {}
                            }
                            player.play()
                        }
                    }
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}