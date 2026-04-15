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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.utils.thumbnail

private data class ThumbnailBounds(
    val collapsedSize: androidx.compose.ui.unit.Dp,
    val expandedSize: androidx.compose.ui.unit.Dp,
    val collapsedRadius: androidx.compose.ui.unit.Dp,
    val expandedRadius: androidx.compose.ui.unit.Dp,
    val collapsedX: androidx.compose.ui.unit.Dp,
    val expandedX: androidx.compose.ui.unit.Dp,
    val collapsedY: androidx.compose.ui.unit.Dp,
    val expandedY: androidx.compose.ui.unit.Dp
)

@Composable
fun SharedThumbnail(expandProgress: Float) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    var mediaItem by remember(player) { mutableStateOf(player.currentMediaItem) }

    var playWhenReady by remember(player) { mutableStateOf(player.playWhenReady) }
    var playbackState by remember(player) { mutableIntStateOf(player.playbackState) }

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
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    val context = LocalContext.current

    val isEffectivelyPlaying = playWhenReady && playbackState != Player.STATE_ENDED

    val playingScale by animateFloatAsState(
        targetValue = if (isEffectivelyPlaying) 1f else 0.75f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "playingScale"
    )

    val finalScale = lerp(1f, playingScale, expandProgress)

    val artworkUrl = remember(mediaItem) {
        mediaItem?.mediaMetadata?.artworkUri?.thumbnail(1024)?.toString()
    }

    val imageRequest = remember(artworkUrl, context) {
        if (artworkUrl.isNullOrBlank()) null else {
            ImageRequest.Builder(context)
                .data(artworkUrl)
                .crossfade(true)
                .size(Size.ORIGINAL)
                .diskCacheKey(artworkUrl)
                .memoryCacheKey(artworkUrl)
                .build()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidth = maxWidth

        val bounds = remember(containerWidth) {
            val expandedSize = containerWidth * 0.85f
            ThumbnailBounds(
                collapsedSize = 40.dp,
                expandedSize = expandedSize,
                collapsedRadius = 20.dp,
                expandedRadius = 32.dp,
                collapsedX = 10.dp,
                expandedX = (containerWidth - expandedSize) / 2,
                collapsedY = 10.dp,
                expandedY = 100.dp
            )
        }

        val currentSize = lerp(bounds.collapsedSize, bounds.expandedSize, expandProgress)
        val cornerRadius = lerp(bounds.collapsedRadius, bounds.expandedRadius, expandProgress)
        val xOffset = lerp(bounds.collapsedX, bounds.expandedX, expandProgress)
        val yOffset = lerp(bounds.collapsedY, bounds.expandedY, expandProgress)

        val (colorPalette, _) = LocalAppearance.current
        val isDarkTheme = colorPalette.background2.luminance() < 0.5f

        val glassColor = if (isDarkTheme) {
            Color.White.copy(alpha = 0.07f)
        } else {
            Color.Black.copy(alpha = 0.04f)
        }

        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = "Album Art",
            modifier = Modifier
                .offset(x = xOffset, y = yOffset)
                .size(currentSize)
                .graphicsLayer {
                    scaleX = finalScale
                    scaleY = finalScale
                    shape = RoundedCornerShape(cornerRadius.toPx())
                    clip = true
                }
                .background(glassColor)
        ) {
            val state by painter.state.collectAsState()

            val dynamicIconSize = lerp(24.dp, 180.dp, expandProgress)

            when (state) {
                is AsyncImagePainter.State.Success -> {
                    Image(
                        painter = painter,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
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
                }
            }
        }
    }
}