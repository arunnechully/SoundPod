package com.github.soundpod.ui.screens.player.old

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.github.soundpod.utils.forceSeekToNext
import com.github.soundpod.utils.forceSeekToPrevious
import com.github.soundpod.utils.miniplayerGesturesEnabledKey
import com.github.soundpod.utils.positionAndDurationState
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.shouldBePlaying
import com.github.soundpod.utils.thumbnail
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    openPlayer: () -> Unit,
    stopPlayer: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var miniplayerGesturesEnabled by rememberPreference(miniplayerGesturesEnabledKey, true)
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
    val positionAndDuration by binder.player.positionAndDurationState()

    val miniPlayerContent: @Composable BoxScope.() -> Unit = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(30.dp))
                .clickable(onClick = openPlayer)
        ) {
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
                            .clip(MaterialTheme.shapes.medium)
                            .size(52.dp)
                    )
                },
                trailingContent = {
                    Row {
                        IconButton(
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
                        ) {
                            Icon(
                                imageVector =
                                    if (shouldBePlaying) Icons.Outlined.Pause
                                    else if (binder.player.playbackState == Player.STATE_ENDED) Icons.Outlined.Replay
                                    else Icons.Outlined.PlayArrow,
                                contentDescription = null,
                            )
                        }

                        IconButton(
                            onClick = stopPlayer
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = null,
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )

            LinearProgressIndicator(
                progress = { positionAndDuration.first.toFloat() / positionAndDuration.second.absoluteValue },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    val startAction = SwipeAction(
        onSwipe = { binder.player.forceSeekToPrevious() },
        icon = {
            Icon(
                imageVector = Icons.Outlined.SkipPrevious,
                contentDescription = null,
                modifier = Modifier.padding(end = 32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        background = MaterialTheme.colorScheme.primaryContainer
    )

    val endAction = SwipeAction(
        onSwipe = { binder.player.forceSeekToNext() },
        icon = {
            Icon(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = null,
                modifier = Modifier.padding(start = 32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        background = MaterialTheme.colorScheme.primaryContainer
    )

    if (miniplayerGesturesEnabled) {
        SwipeableActionsBox(
            startActions = listOf(startAction),
            endActions = listOf(endAction),
            content = miniPlayerContent
        )
    } else {
        Box(content = miniPlayerContent)
    }
}