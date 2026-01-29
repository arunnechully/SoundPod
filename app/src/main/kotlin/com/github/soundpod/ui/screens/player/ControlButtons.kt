@file:kotlin.OptIn(ExperimentalAnimationApi::class)

package com.github.soundpod.ui.screens.player

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.core.ui.favoritesIcon
import com.github.core.ui.surface
import com.github.innertube.models.NavigationEndpoint
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.ProgressBar
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Song
import com.github.soundpod.query
import com.github.soundpod.ui.components.BaseMediaItemMenu
import com.github.soundpod.ui.components.SeekBar
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.forceSeekToNext
import com.github.soundpod.utils.forceSeekToPrevious
import com.github.soundpod.utils.formatAsDuration
import com.github.soundpod.utils.queueLoopEnabledKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.seamlessPlay
import com.github.soundpod.utils.shuffleQueue
import com.github.soundpod.utils.toast
import com.github.soundpod.utils.trackLoopEnabledKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.7f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 50f
        ),
        label = "smooth-bounce-button"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}


@Composable
fun PlayPauseButton(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current

    AnimatedIconButton(
        onClick = onClick,
        modifier = modifier
            .semantics { contentDescription = if (playing) "Pause" else "Play" }
    ) {
        Icon(
            painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.play),
            contentDescription = null,
            tint = colorPalette.iconColor,
            modifier = Modifier
                .size(30.dp)
        )
    }
}

@Composable
fun ShufflePlayButtons(
    onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {

        //shuffle button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(shape = CircleShape)
                .background(colorPalette.surface)
                .clickable(onClick = onShuffle), contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.shuffle),
                contentDescription = "Shuffle",
                tint = colorPalette.iconColor,
                modifier = Modifier.size(18.dp)

            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Play button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(shape = CircleShape)
                .background(colorPalette.surface)
                .clickable(onClick = onPlay), contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.play),
                contentDescription = "Play",
                tint = colorPalette.iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun MiniPlayerControl(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val (colorPalette) = LocalAppearance.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
    ) {
        //skip back button
        AnimatedIconButton(
            onClick = { binder?.player?.forceSeekToPrevious() }, modifier = modifier.size(42.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_backward),
                contentDescription = null,
                tint = colorPalette.iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        //play or pause button
        AnimatedIconButton(
            onClick = onClick,
            modifier = modifier
                .semantics { contentDescription = if (playing) "Pause" else "Play" }
        ) {
            Icon(
                painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = colorPalette.iconColor,
                modifier = Modifier
                    .size(28.dp)
            )
        }

        //skip forward button
        AnimatedIconButton(
            onClick = { binder?.player?.forceSeekToNext() }, modifier = modifier.size(42.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_forward),
                contentDescription = null,
                tint = colorPalette.iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PlayerMiddleControl(
    showPlaylist: Boolean,
    onTogglePlaylist: (Boolean) -> Unit,
    mediaId: String
) {
    val binder = LocalPlayerServiceBinder.current
    val (colorPalette) = LocalAppearance.current
    var likedAt by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(mediaId) {
        db.likedAt(mediaId).distinctUntilChanged().collect { likedAt = it }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        AnimatedIconButton(
            onClick = { onTogglePlaylist(!showPlaylist) }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.playlist),
                contentDescription = "Playlist",
                tint = colorPalette.iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedIconButton(
            onClick = {
                val currentMediaItem = binder?.player?.currentMediaItem
                query {
                    if (db.like(
                            mediaId,
                            if (likedAt == null) System.currentTimeMillis() else null
                        ) == 0
                    ) {
                        currentMediaItem
                            ?.takeIf { it.mediaId == mediaId }
                            ?.let {
                                db.insert(currentMediaItem, Song::toggleLike)
                            }
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(id = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart),
                contentDescription = "Like",
                tint = (if (likedAt == null) colorPalette.iconColor else colorPalette.favoritesIcon),
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedIconButton(
            onClick = {/*todo*/},
        ) {
            Icon(
                painter = painterResource(id = R.drawable.add),
                contentDescription = "Add",
                tint = colorPalette.iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun PlayerControlBottom(
    shouldBePlaying: Boolean,
    onPlayPauseClick: () -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val (colorPalette) = LocalAppearance.current
    var trackLoopEnabled by rememberPreference(trackLoopEnabledKey, defaultValue = false)
    var queueLoopEnabled by rememberPreference(queueLoopEnabledKey, defaultValue = false)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        // Shuffle
        AnimatedIconButton(
            onClick = {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                player.shuffleQueue()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.shuffle),
                contentDescription = "Shuffle",
                tint = colorPalette.iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // Previous
        AnimatedIconButton(
            onClick = { binder.player.forceSeekToPrevious() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_backward),
                contentDescription = "Previous",
                tint = colorPalette.iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Play / Pause
        PlayPauseButton(
            playing = shouldBePlaying,
            onClick = onPlayPauseClick
        )

        // Next
        AnimatedIconButton(
            onClick = { binder.player.forceSeekToNext() },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_forward),
                contentDescription = "Next",
                tint = colorPalette.iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Repeat
        AnimatedIconButton(
            onClick = {
                if (trackLoopEnabled) {
                    trackLoopEnabled = false
                } else if (queueLoopEnabled) {
                    queueLoopEnabled = false
                    trackLoopEnabled = true
                } else {
                    queueLoopEnabled = true
                }
            }
        ) {
            val repeatMode = when {
                trackLoopEnabled -> Player.REPEAT_MODE_ONE
                queueLoopEnabled -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            val icon = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> painterResource(R.drawable.repeat_one)
                Player.REPEAT_MODE_ALL -> painterResource(R.drawable.repeat)
                else -> painterResource(R.drawable.repeat_off)
            }
            val alpha = if (repeatMode == Player.REPEAT_MODE_OFF) Dimensions.lowOpacity else 1f

            Icon(
                painter = icon,
                tint = colorPalette.iconColor,
                contentDescription = null,
                modifier = Modifier
                    .alpha(alpha)
                    .size(28.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@UnstableApi
@Composable
fun PlayerTopControl(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onBack: () -> Unit
) {
    val menuState = LocalMenuState.current
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var nullableMediaItem by remember {
        mutableStateOf(
            binder.player.currentMediaItem,
            neverEqualPolicy()
        )
    }
    val context = LocalContext.current

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    val mediaItem = nullableMediaItem ?: return

    var isShowingSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    val sleepTimerMillisLeft by (binder.sleepTimerMillisLeft
        ?: flowOf(null))
        .collectAsState(initial = null)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 24.dp)
    ) {

        IconButton(
            onClick = onBack,
        ){
            Icon(
                painter = painterResource(id = R.drawable.arrow_down),
                tint = colorPalette.iconColor,
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (sleepTimerMillisLeft != null) {

            Text(
                text = formatTime(sleepTimerMillisLeft ?: 0L),
                color =colorPalette.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        isShowingSleepTimerDialog = true
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )

        } else {

            IconButton(
                onClick = { isShowingSleepTimerDialog = true },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    tint = colorPalette.iconColor,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        IconButton(
            onClick = {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder.player.audioSessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }

                try {
                    activityResultLauncher.launch(intent)
                } catch (_: ActivityNotFoundException) {
                    context.toast("Couldn't find an application to equalize audio")
                }
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.equalizer),
                tint = colorPalette.iconColor,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
            )
        }

        IconButton(
            onClick = {
                menuState.display {
                    BaseMediaItemMenu(
                        onDismiss = menuState::hide,
                        mediaItem = mediaItem,
                        onStartRadio = {
                            binder.stopRadio()
                            binder.player.seamlessPlay(mediaItem)
                            binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
                        },
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist
                    )
                }
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                tint = colorPalette.iconColor,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
            )
        }
    }
    if (isShowingSleepTimerDialog) {
        SleepTimer(
            sleepTimerMillisLeft = sleepTimerMillisLeft,
            onDismiss = { isShowingSleepTimerDialog = false }
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}


@Composable
fun PlayerSeekBar(
    mediaId: String,
    position: Long,
    duration: Long,
    progressBarStyle: ProgressBar
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    when (progressBarStyle) {

        ProgressBar.Default -> {
            PlayerSeekBarDefault(
                mediaId = mediaId,
                position = position,
                duration = duration
            )
        }

        ProgressBar.Animated -> {
            PlayerSeekBarAnimated(
                mediaId = mediaId,
                position = position,
                duration = duration
            )
        }
    }
}

@Composable
private fun PlayerSeekBarDefault(
    mediaId: String,
    position: Long,
    duration: Long
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var scrubbingPosition by remember(mediaId) { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {

        SeekBar(
            value = scrubbingPosition ?: position,
            minimumValue = 0,
            maximumValue = duration,
            onDragStart = { scrubbingPosition = it },
            onDrag = { delta ->
                scrubbingPosition = if (duration != C.TIME_UNSET) {
                    scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                } else null
            },
            onDragEnd = {
                scrubbingPosition?.let(binder.player::seekTo)
                scrubbingPosition = null
            },
            color = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formatAsDuration(scrubbingPosition ?: position),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )

            if (duration != C.TIME_UNSET) {
                Text(
                    text = formatAsDuration(duration),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PlayerSeekBarAnimated(
    mediaId: String,
    position: Long,
    duration: Long
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var scrubbingPosition by remember(mediaId) { mutableStateOf<Float?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {

        AnimatedSeekbar(
            value = scrubbingPosition ?: position.toFloat(),
            onValueChange = { scrubbingPosition = it },
            onValueChangeFinished = {
                scrubbingPosition?.let { binder.player.seekTo(it.toLong()) }
                scrubbingPosition = null
            },
            valueRange = 0f..duration.toFloat(),
            isPlaying = binder.player.isPlaying
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formatAsDuration((scrubbingPosition ?: position.toFloat()).toLong()),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium
            )

            if (duration != C.TIME_UNSET) {
                Text(
                    text = formatAsDuration(duration),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
