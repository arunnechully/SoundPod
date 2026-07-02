@file:kotlin.OptIn(ExperimentalAnimationApi::class)

package com.github.soundpod.ui.screens.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.PlayerLayout
import com.github.soundpod.enums.ProgressBar
import com.github.soundpod.models.Song
import com.github.soundpod.query
import com.github.soundpod.ui.components.CustomDropdownMenu
import com.github.soundpod.ui.screens.player.seekbar.PaperBoatAnimation
import com.github.soundpod.ui.screens.player.seekbar.SeekBar
import com.github.soundpod.ui.screens.player.seekbar.SimpleWave
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.forceSeekToNext
import com.github.soundpod.utils.forceSeekToPrevious
import com.github.soundpod.utils.formatAsDuration
import com.github.soundpod.utils.playerlayout
import com.github.soundpod.utils.queueLoopEnabledKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.shuffleQueue
import com.github.soundpod.utils.toast
import com.github.soundpod.utils.trackLoopEnabledKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

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

@Suppress("KotlinConstantConditions")
@Composable
private fun HoldableAnimatedIconButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onHold: (suspend () -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var isLongPress by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(400.milliseconds)
            if (isPressed && onHold != null) {
                isLongPress = true
                onHold()
            }
        } else {
            if (isLongPress) {
                delay(200.milliseconds)
                isLongPress = false
            }
        }
    }

    AnimatedIconButton(
        modifier = modifier,
        onClick = { if (!isLongPress) onClick() },
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}


@Composable
fun PlayPauseButton(
    modifier: Modifier = Modifier,
    playing: Boolean,
    isBuffering: Boolean = false,
    onClick: () -> Unit,

    ) {
    val (colorPalette) = LocalAppearance.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(58.dp)
    ) {
        AnimatedIconButton(
            onClick = onClick,
            modifier = Modifier
                .semantics { contentDescription = if (playing) "Pause" else "Play" }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    tint = colorPalette.iconColor.copy(alpha = if (isBuffering) 0.5f else 1f),
                    modifier = Modifier.size(38.dp)
                )
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(52.dp),
                        color = colorPalette.iconColor,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
fun NewPlayPauseButton(
    modifier: Modifier = Modifier,
    playing: Boolean,
    isBuffering: Boolean = false,
    onClick: () -> Unit,
    color: Color = LocalAppearance.current.colorPalette.iconColor
) {
    val (colorPalette) = LocalAppearance.current
    val infiniteTransition = rememberInfiniteTransition(label = "organic_dialer_button")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "global_rotation"
    )

    val squiggleIntensity by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = tween(1000, easing = EaseInOutSine),
        label = "squiggle_intensity"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(74.dp)
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isBuffering) 0.5f else 1f)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2.2f
            val path = Path()

            val points = 200
            for (i in 0..points) {
                val angleInRadians = (i.toFloat() / points) * 2f * PI.toFloat()

                val waveCount = 16f
                val amplitude = 1.1.dp.toPx()

                val wave = sin(angleInRadians * waveCount + phase) * amplitude * squiggleIntensity
                val currentRadius = baseRadius + wave

                val totalRotation = (rotation * PI.toFloat() / 180f)
                val x = center.x + currentRadius * cos(angleInRadians + totalRotation)
                val y = center.y + currentRadius * sin(angleInRadians + totalRotation)

                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(
                path = path,
                color = color
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = colorPalette.background4.copy(alpha = if (isBuffering) 0.5f else 1f),
                modifier = Modifier.size(34.dp)
            )
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = colorPalette.background4,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MiniPlayerControl(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val (colorPalette) = LocalAppearance.current

    var playbackState by remember { mutableIntStateOf(player.playbackState) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    val isBuffering = playbackState == Player.STATE_BUFFERING &&
            (player.currentMediaItem?.mediaId?.let {
                binder.isCached(
                    it,
                    player.currentPosition,
                    1024L
                )
            } != true)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
    ) {
        //skip back button
        HoldableAnimatedIconButton(
            onClick = { player.forceSeekToPrevious() },
            onHold = {
                val duration = player.duration
                val skipAmount = if (duration != C.TIME_UNSET && duration > 0) {
                    (duration / 100).coerceIn(1000L, 10000L)
                } else {
                    1000L
                }
                while (true) {
                    player.seekTo((player.currentPosition - skipAmount).coerceAtLeast(0))
                    delay(100.milliseconds)
                }
            },
            modifier = modifier.size(42.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_backward),
                contentDescription = null,
                tint = colorPalette.iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(48.dp)
        ) {
            AnimatedIconButton(
                onClick = onClick,
                modifier = Modifier
                    .semantics { contentDescription = if (playing) "Pause" else "Play" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = colorPalette.iconColor.copy(alpha = if (isBuffering) 0.5f else 1f),
                        modifier = Modifier.size(28.dp)
                    )
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = colorPalette.iconColor,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        //skip forward button
        HoldableAnimatedIconButton(
            onClick = { player.forceSeekToNext() },
            onHold = {
                val originalParameters = player.playbackParameters
                try {
                    player.playbackParameters = PlaybackParameters(4f, originalParameters.pitch)
                    delay(Long.MAX_VALUE.milliseconds)
                } finally {
                    player.playbackParameters = originalParameters
                }
            },
            modifier = modifier.size(42.dp)
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
    val ctx = LocalContext.current
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
                tint = (colorPalette.iconColor),
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedIconButton(
            onClick = {
                Toast.makeText(ctx, "Function not setup yet", Toast.LENGTH_SHORT).show()
            },
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

@OptIn(UnstableApi::class)
@Composable
fun PlayerControlBottom(
    shouldBePlaying: Boolean,
    onPlayPauseClick: () -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val (colorPalette) = LocalAppearance.current

    val playerLayout by rememberPreference(playerlayout, PlayerLayout.Default)

    var trackLoopEnabled by rememberPreference(trackLoopEnabledKey, defaultValue = false)
    var queueLoopEnabled by rememberPreference(queueLoopEnabledKey, defaultValue = false)

    var playbackState by remember { mutableIntStateOf(player.playbackState) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    val isBuffering = playbackState == Player.STATE_BUFFERING &&
            (player.currentMediaItem?.mediaId?.let {
                binder.isCached(
                    it,
                    player.currentPosition,
                    1024L
                )
            } != true)

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
        HoldableAnimatedIconButton(
            onClick = { player.forceSeekToPrevious() },
            onHold = {
                val duration = player.duration
                val skipAmount = if (duration != C.TIME_UNSET && duration > 0) {
                    (duration / 100).coerceIn(1000L, 10000L)
                } else {
                    1000L
                }
                while (true) {
                    player.seekTo((player.currentPosition - skipAmount).coerceAtLeast(0))
                    delay(100.milliseconds)
                }
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_backward),
                contentDescription = "Previous",
                tint = colorPalette.iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Play/Pause Control (Crossfade Removed)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(if (playerLayout == PlayerLayout.New) 84.dp else 72.dp)
        ) {
            if (playerLayout == PlayerLayout.New) {
                NewPlayPauseButton(
                    playing = shouldBePlaying,
                    isBuffering = isBuffering, // Passed directly
                    onClick = onPlayPauseClick
                )
            } else if (playerLayout == PlayerLayout.Default) {
                PlayPauseButton(
                    playing = shouldBePlaying,
                    isBuffering = isBuffering, // Passed directly
                    onClick = onPlayPauseClick
                )
            }
        }

        // Next
        HoldableAnimatedIconButton(
            onClick = { player.forceSeekToNext() },
            onHold = {
                val originalParameters = player.playbackParameters
                try {
                    player.playbackParameters = PlaybackParameters(4f, originalParameters.pitch)
                    delay(Long.MAX_VALUE.milliseconds)
                } finally {
                    player.playbackParameters = originalParameters
                }
            }
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
            val alpha = if (repeatMode == Player.REPEAT_MODE_OFF) Dimensions.LOWOPACITY else 1f

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
    onGoToAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    onTrackDetailsClick: () -> Unit = {},
    onBack: () -> Unit,
    onLyricsClick: () -> Unit = {},
    isPlaylistShowing: Boolean,
    onSettingsClick: () -> Unit,
    onSleepTimerClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    val context = LocalContext.current
    
    val mediaId = player.currentMediaItem?.mediaId ?: ""
    val isDownloaded by (if (mediaId.isNotEmpty()) {
        binder.downloadManager.isDownloaded(mediaId)
    } else {
        kotlinx.coroutines.flow.flowOf(false)
    }).collectAsState(initial = false)

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    val sleepTimerMillisLeft by binder.sleepTimerMillisLeft.collectAsState(initial = null)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = 15.dp,
            )
    ) {

        IconButton(
            onClick = onBack,
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPlaylistShowing) R.drawable.arrow_back else R.drawable.arrow_down
                ),
                tint = colorPalette.iconColor,
                contentDescription = if (isPlaylistShowing) "Close Playlist" else "Minimize Player",
                modifier = Modifier
                    .size(22.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (sleepTimerMillisLeft != null) {
            Text(
                text = formatTime(sleepTimerMillisLeft ?: 0L),
                color = colorPalette.text,
                style = typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onSleepTimerClick()
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        } else {
            IconButton(
                onClick = onSleepTimerClick,
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
                onLyricsClick()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.music_lyric),
                tint = colorPalette.iconColor,
                contentDescription = "music lyrics",
                modifier = Modifier
                    .size(22.dp)
            )
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

        var showDropDown by remember { mutableStateOf(false) }

        Box(
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = { showDropDown = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = colorPalette.text,
                    modifier = Modifier.size(24.dp)
                )
            }
            CustomDropdownMenu(
                expanded = showDropDown,
                onDismissRequest = { showDropDown = false },
                endPadding = 0.dp
            ) {
                if (onGoToAlbum != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(id = R.string.album),
                                color = colorPalette.text,
                                style = typography.bodyLarge
                            )
                        },
                        onClick = {
                            showDropDown = false
                            onGoToAlbum()
                        }
                    )
                }
                if (onGoToArtist != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(id = R.string.artist),
                                color = colorPalette.text,
                                style = typography.bodyLarge
                            )
                        },
                        onClick = {
                            showDropDown = false
                            onGoToArtist()
                        }
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = if (isDownloaded) R.string.remove_download else R.string.download),
                            color = colorPalette.text,
                            style = typography.bodyLarge
                        )
                    },
                    onClick = {
                        showDropDown = false
                        if (isDownloaded) {
                            binder.removeDownload(mediaId)
                        } else {
                            player.currentMediaItem?.let { binder.downloadManager.download(it) }
                        }
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.track_details),
                            color = colorPalette.text,
                            style = typography.bodyLarge
                        )
                    },
                    onClick = {
                        showDropDown = false
                        onTrackDetailsClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.settings),
                            color = colorPalette.text,
                            style = typography.bodyLarge
                        )
                    },
                    onClick = {
                        showDropDown = false
                        onSettingsClick()
                    }
                )

            }
        }
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
    progressBarStyle: ProgressBar,
    isPlaying: Boolean,
    onDraggingStateChange: (Boolean) -> Unit = {}
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    when (progressBarStyle) {

        ProgressBar.Default -> {
            PlayerSeekBarDefault(
                mediaId = mediaId,
                position = position,
                duration = duration,
                onDraggingStateChange = onDraggingStateChange
            )
        }

        ProgressBar.Wave -> {
            WaveAnimation(
                mediaId = mediaId,
                position = position,
                duration = duration,
                isPlaying = isPlaying
            )
        }

        ProgressBar.Paperboat -> {
            BoatAnimation(
                mediaId = mediaId,
                position = position,
                duration = duration,
                isPlaying = isPlaying
            )
        }
    }
}

@Composable
private fun PlayerSeekBarDefault(
    mediaId: String,
    position: Long,
    duration: Long,
    onDraggingStateChange: (Boolean) -> Unit = {}
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var scrubbingPosition by remember(mediaId) { mutableStateOf<Long?>(null) }
    val isDragging = scrubbingPosition != null
    val currentDisplayPosition = scrubbingPosition ?: position


    LaunchedEffect(isDragging) {
        onDraggingStateChange(isDragging)
    }

    val floatingAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "FloatingTextFade"
    )

    val bottomLabelsAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "BottomLabelsFade"
    )
    val floatingScale by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "FloatingTextScale"
    )

    val progressFraction = if (duration > 0) {
        currentDisplayPosition.toFloat() / duration
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxWidthDp = maxWidth

            if (floatingAlpha > 0f) {
                val textWidth = 46.dp
                val halfTextWidth = textWidth / 2
                val rawXOffset = (progressFraction * maxWidthDp.value).dp - halfTextWidth
                val clampedXOffset = rawXOffset.coerceIn(0.dp, maxWidthDp - textWidth)

                Text(
                    text = formatAsDuration(currentDisplayPosition),
                    color = LocalAppearance.current.colorPalette.text,
                    style = typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .offset(
                            x = clampedXOffset,
                            y = (-28).dp
                        )
                        .graphicsLayer {
                            alpha = floatingAlpha
                            scaleX = floatingScale
                            scaleY = floatingScale
                        }
                )
            }

            SeekBar(
                value = currentDisplayPosition,
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
                color = LocalAppearance.current.colorPalette.text,
                backgroundColor = LocalAppearance.current.colorPalette.text.copy(alpha = 0.07f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = formatAsDuration(position),
                color = MaterialTheme.colorScheme.onSurface,
                style = typography.labelMedium,
                maxLines = 1,
                modifier = Modifier.graphicsLayer { alpha = bottomLabelsAlpha }
            )

            if (duration != C.TIME_UNSET) {
                Text(
                    text = formatAsDuration(duration),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer { alpha = bottomLabelsAlpha }
                )
            }
        }
    }
}

@Composable
private fun WaveAnimation(
    mediaId: String,
    position: Long,
    duration: Long,
    isPlaying: Boolean
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var scrubbingPosition by remember(mediaId) { mutableStateOf<Float?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {

        SimpleWave(
            value = scrubbingPosition ?: position.toFloat(),
            onValueChange = { scrubbingPosition = it },
            onValueChangeFinished = {
                scrubbingPosition?.let { binder.player.seekTo(it.toLong()) }
                scrubbingPosition = null
            },
            valueRange = 0f..duration.toFloat(),
            isPlaying = isPlaying
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
                style = typography.labelMedium
            )

            if (duration != C.TIME_UNSET) {
                Text(
                    text = formatAsDuration(duration),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun BoatAnimation(
    mediaId: String,
    position: Long,
    duration: Long,
    isPlaying: Boolean
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var scrubbingPosition by remember(mediaId) { mutableStateOf<Float?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {

        PaperBoatAnimation(
            value = scrubbingPosition ?: position.toFloat(),
            onValueChange = { scrubbingPosition = it },
            onValueChangeFinished = {
                scrubbingPosition?.let { binder.player.seekTo(it.toLong()) }
                scrubbingPosition = null
            },
            valueRange = 0f..duration.toFloat(),
            isPlaying = isPlaying
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
                style = typography.labelMedium
            )

            if (duration != C.TIME_UNSET) {
                Text(
                    text = formatAsDuration(duration),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = typography.labelMedium
                )
            }
        }
    }
}