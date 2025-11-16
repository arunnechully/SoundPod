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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.innertube.models.NavigationEndpoint
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.BaseMediaItemMenu
import com.github.soundpod.utils.forceSeekToNext
import com.github.soundpod.utils.forceSeekToPrevious
import com.github.soundpod.utils.seamlessPlay
import com.github.soundpod.utils.toast

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
//    val (colorPalette) = LocalAppearance.current

    AnimatedIconButton(
        onClick = onClick,
        modifier = modifier
            .semantics { contentDescription = if (playing) "Pause" else "Play" }
    ) {
        Icon(
            painter = painterResource(id = if (playing) R.drawable.pause else R.drawable.play),
            contentDescription = null,
//            tint = colorPalette.text,
            modifier = Modifier
                .size(30.dp)
        )
    }
}

@Composable
fun ShufflePlayButtons(
    onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier
) {
//    val (colorPalette, _) = LocalAppearance.current
//    val surfaceColor = AppearancePreferences.getSurfaceColor()
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
//                .background(surfaceColor)
                .clickable(onClick = onShuffle), contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.shuffle),
                contentDescription = "Shuffle",
//                tint = colorPalette.text,
                modifier = Modifier.size(18.dp)

            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Play button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(shape = CircleShape)
//                .background(surfaceColor)
                .clickable(onClick = onPlay), contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.play),
                contentDescription = "Play",
//                tint = colorPalette.text,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun BottomPlayerControl(
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
                tint = colorPalette.text,
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
                tint = colorPalette.text,
                modifier = Modifier
                    .size(22.dp)
            )
        }

        //skip forward button
        AnimatedIconButton(
            onClick = { binder?.player?.forceSeekToNext() }, modifier = modifier.size(42.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.fast_forward),
                contentDescription = null,
                tint = colorPalette.text,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PlayerMiddleControl(
    likedAt: Long?,
    setLikedAt: (Long?) -> Unit,
    showPlaylist: Boolean,
    onTogglePlaylist: (Boolean) -> Unit
) {
    var showList by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        AnimatedIconButton(
            onClick = { onTogglePlaylist(!showPlaylist) }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.playlist),
                contentDescription = "Playlist",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedIconButton(onClick = {
            setLikedAt(if (likedAt == null) System.currentTimeMillis() else null)
        }) {
            Icon(
                painter = painterResource(id = if (likedAt == null) R.drawable.heart_outline else R.drawable.heart),
                contentDescription = "Like",
                tint = (if (likedAt == null) textColor else Color(0x00EA0838)),
                modifier = Modifier.size(24.dp)
            )
        }

        AnimatedIconButton(
            onClick = { showList = true },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.add),
                contentDescription = "Add",
                tint = textColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
//    Addtolist(
//        showSheet = showList,
//        onDismiss = { showList = false },
//        onAddToPlaylistClick = {},
//        onCreatePlaylistClick = {},
//    )
}

//@Composable
//fun PlayerControlBottom(
//    colorPalette: ColorPalette,
//    shouldBePlaying: Boolean,
//) {
//    val binder = LocalPlayerServiceBinder.current
//    var isShuffleActive by remember { mutableStateOf(false) }
//    var trackLoopEnabled by remember { mutableStateOf(PlayerPreferences.trackLoopEnabled) }
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween,
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp)
//    ) {
//        // Shuffle
//        AnimatedIconButton(
//            onClick = {
//                isShuffleActive = !isShuffleActive
//                binder?.player?.playRandomSong()
//            }
//        ) {
//            Icon(
//                painter = painterResource(id = R.drawable.shuffle),
//                contentDescription = "Shuffle",
//                tint = colorPalette.text,
//                modifier = Modifier.size(24.dp)
//            )
//        }
//
//        // Previous
//        AnimatedIconButton(
//            onClick = { binder?.player?.forceSeekToPrevious() }
//        ) {
//            Icon(
//                painter = painterResource(id = R.drawable.fast_backward),
//                contentDescription = "Previous",
//                tint = colorPalette.text,
//                modifier = Modifier.size(18.dp)
//            )
//        }
//
//        // Play / Pause
//        PlayPauseButton(
//            playing = shouldBePlaying,
//            onClick = {
//                if (shouldBePlaying) binder?.player?.pause()
//                else {
//                    if (binder?.player?.playbackState == Player.STATE_IDLE) {
//                        binder.player.prepare()
//                    }
//                    binder?.player?.play()
//                    Modifier
//                        .size(42.dp)
//                }
//            }
//        )
//
//        // Next
//        AnimatedIconButton(
//            onClick = { binder?.player?.forceSeekToNext() },
//        ) {
//            Icon(
//                painter = painterResource(id = R.drawable.fast_forward),
//                contentDescription = "Next",
//                tint = colorPalette.text,
//                modifier = Modifier.size(18.dp)
//            )
//        }
//
//        // Repeat
//        AnimatedIconButton(
//            onClick = {
//                val newValue = !trackLoopEnabled
//                trackLoopEnabled = newValue
//                PlayerPreferences.trackLoopEnabled = newValue
//            }
//        ) {
//            Icon(
//                painter = (if (trackLoopEnabled) painterResource(id = R.drawable.repeat_enabled) else painterResource(
//                    id = R.drawable.repeat_disabled
//                )),
//                contentDescription = "Repeat",
//                tint = colorPalette.text,
//                modifier = Modifier.size(24.dp)
//            )
//        }
//    }
//}

@OptIn(UnstableApi::class)
@UnstableApi
@Composable
fun PlayerTopControl(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val menuState = LocalMenuState.current
    var showPlaylist by remember { mutableStateOf(false) }

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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp, vertical = 24.dp)
    ) {

        IconButton(
            onClick = {},
        ){
            Icon(
                painter = painterResource(id = R.drawable.arrow_down),
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
            )
        }

        Spacer(modifier = Modifier.weight(0.7f))

        IconButton(
            onClick = {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder?.player?.audioSessionId)
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
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
            )
        }
    }
}