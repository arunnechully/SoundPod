package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.thumbnail

@Composable
fun SharedThumbnail(expandProgress: Float) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    var mediaItem by remember { mutableStateOf(player.currentMediaItem) }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(newItem: MediaItem?, reason: Int) {
                mediaItem = newItem
            }
        }
    }

    val context = LocalContext.current

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val bounds = remember(windowInfo.containerSize.width, density.density) {

        val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
        val expandedSize = screenWidth * 0.85f

        object {
            val collapsedSize = 40.dp
            val expandedSize = expandedSize
            val collapsedRadius = 20.dp
            val expandedRadius = 32.dp
            val collapsedX = 10.dp
            val expandedX = (screenWidth - expandedSize) / 2
            val collapsedY = 10.dp
            val expandedY = 120.dp
        }
    }

    val currentSize = lerp(bounds.collapsedSize, bounds.expandedSize, expandProgress)
    val cornerRadius = lerp(bounds.collapsedRadius, bounds.expandedRadius, expandProgress)
    val xOffset = lerp(bounds.collapsedX, bounds.expandedX, expandProgress)
    val yOffset = lerp(bounds.collapsedY, bounds.expandedY, expandProgress)

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

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = imageRequest,
            placeholder = painterResource(id = R.drawable.app_icon),
            error = painterResource(id = R.drawable.app_icon),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .offset(x = xOffset, y = yOffset)
                .size(currentSize)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.DarkGray)
        )
    }
}