package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.shouldBePlaying
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayerContent(
    openPlayer: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val (colorPalette) = LocalAppearance.current

    var shouldBePlaying by remember { mutableStateOf(player.shouldBePlaying) }

    var nullableMediaItem by remember {
        mutableStateOf(player.currentMediaItem, neverEqualPolicy())
    }

    var position by remember { mutableLongStateOf(player.currentPosition) }
    var duration by remember { mutableLongStateOf(player.duration) }

    LaunchedEffect(player, shouldBePlaying) {
        if (shouldBePlaying) {
            while (isActive) {
                position = player.currentPosition
                duration = player.duration
                delay(1000)
            }
        }
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

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                position = newPosition.positionMs
            }
        }
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (nullableMediaItem != null) openPlayer() }
            )
    ) {
        ListItem(
            headlineContent = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = nullableMediaItem?.mediaMetadata?.title?.toString() ?: "SoundPod",
                        modifier = Modifier.basicMarquee(),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        color = colorPalette.text
                    )
                }
            },
            supportingContent = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = nullableMediaItem?.mediaMetadata?.artist?.toString() ?: "Not playing",
                        modifier = Modifier.basicMarquee(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colorPalette.text.copy(alpha = 0.7f)
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
                        if (nullableMediaItem != null) {
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
                    }
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )

        if (duration > 0) {
            LinearProgressIndicator(
                progress = { position.toFloat() / duration.coerceAtLeast(1L) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = colorPalette.text.copy(alpha = 0.4f),
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
