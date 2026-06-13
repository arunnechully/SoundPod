package com.github.soundpod.ui.screens.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.utils.thumbnail

@Composable
fun SharedThumbnail(
    expandProgress: Float,
    isLandscape: Boolean = false
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    var mediaItem by remember(player) { mutableStateOf(player.currentMediaItem) }
    var playWhenReady by remember(player) { mutableStateOf(player.playWhenReady) }
    var playbackState by remember(player) { mutableIntStateOf(player.playbackState) }
    var playerError by remember(player) { mutableStateOf<PlaybackException?>(player.playerError) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(newItem: MediaItem?, reason: Int) {
                mediaItem = newItem
            }
            override fun onPlayWhenReadyChanged(playWhenReadyState: Boolean, reason: Int) {
                playWhenReady = playWhenReadyState
            }
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state != Player.STATE_IDLE) {
                    playerError = null
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                playerError = error
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val context = LocalContext.current
    val isEffectivelyPlaying = playWhenReady && playbackState != Player.STATE_ENDED

    val playingScale by animateFloatAsState(
        targetValue = if (isEffectivelyPlaying) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "playingScale"
    )

    val finalScale = lerp(1f, playingScale, expandProgress)

    val artworkUrl = remember(mediaItem) {
        mediaItem?.mediaMetadata?.artworkUri?.thumbnail(1024)?.toString()
    }

    val imageRequest = remember(artworkUrl, context) {
        ImageRequest.Builder(context)
            .data(artworkUrl)
            .crossfade(true)
            .size(Size.ORIGINAL)
            .diskCacheKey(artworkUrl)
            .memoryCacheKey(artworkUrl)
            .build()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        
        val expandedSize = if (isLandscape) {
            (containerHeight * 0.8f).coerceAtMost(containerWidth * 0.45f)
        } else {
            containerWidth * 0.85f
        }
        val collapsedSize = 40.dp
        
        val expandedRadius = 32.dp
        val collapsedRadius = 20.dp
        
        val expandedX = if (isLandscape) {
            (containerWidth * 0.5f - expandedSize) / 2
        } else {
            (containerWidth - expandedSize) / 2
        }
        val collapsedX = 10.dp
        
        val expandedY = if (isLandscape) {
            (containerHeight - expandedSize) / 2
        } else {
            100.dp
        }
        val collapsedY = 10.dp

        val (colorPalette, _) = LocalAppearance.current
        val isDarkTheme = colorPalette.background2.luminance() < 0.5f
        val glassColor = if (isDarkTheme) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f)

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = lerp(collapsedX, expandedX, expandProgress).roundToPx(),
                        y = lerp(collapsedY, expandedY, expandProgress).roundToPx()
                    )
                }
                .size(lerp(collapsedSize, expandedSize, expandProgress))
                .graphicsLayer {
                    scaleX = finalScale
                    scaleY = finalScale
                    shape = RoundedCornerShape(lerp(collapsedRadius, expandedRadius, expandProgress).toPx())
                    clip = true
                }
                .background(glassColor)
        ) {
            // Placeholder Layer
            val dynamicIconSize = lerp(24.dp, 180.dp, expandProgress)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(dynamicIconSize)
                )
            }

            // Image Layer
            AsyncImage(
                model = imageRequest,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (expandProgress > 0.8f) {
                PlaybackError(
                    error = playerError,
                    onDismiss = { player.prepare() },
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}
