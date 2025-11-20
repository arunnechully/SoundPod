package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.ui.styling.px
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.shouldBePlaying
import com.github.soundpod.utils.thumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMiniPlayer(
    openPlayer: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var shouldBePlaying by remember { mutableStateOf(binder.player.shouldBePlaying) }

    var nullableMediaItem by remember {
        mutableStateOf(binder.player.currentMediaItem, neverEqualPolicy())
    }

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val mediaItem = nullableMediaItem ?: return

        Column(
            modifier = Modifier
                .clickable(onClick = openPlayer)
        )
        {
            ListItem(
                headlineContent = {
                    Text(
                        text = mediaItem.mediaMetadata.title?.toString() ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        text = mediaItem.mediaMetadata.artist?.toString() ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    AsyncImage(
                        model = mediaItem.mediaMetadata.artworkUri.thumbnail(Dimensions.thumbnails.song.px),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .size(48.dp)
                    )
                },
                trailingContent = {
                    MiniPlayerControl(
                        playing = shouldBePlaying,
                        onClick = {
                            if (shouldBePlaying) binder.player.pause()
                            else {
                                if (binder.player.playbackState == Player.STATE_IDLE) {
                                    binder.player.prepare()
                                } else if (binder.player.playbackState == Player.STATE_ENDED) {
                                    binder.player.seekToDefaultPosition(0)
                                }
                                binder.player.play()
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