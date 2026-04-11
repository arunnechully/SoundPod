package com.github.soundpod.ui.screens.player

import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.currentWindow
import com.github.soundpod.utils.thumbnail

@Composable
fun SharedThumbnail(expandProgress: Float) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    var nullableWindow by remember { mutableStateOf(player.currentWindow) }
    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableWindow = player.currentWindow
            }
        }
    }

    val window = nullableWindow ?: return
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val context = LocalContext.current

    val totalSize = lerp(48.dp, screenWidth * 0.8f, expandProgress)
    val cornerRadius = lerp(24.dp, 32.dp, expandProgress)
    val xOffset = lerp(16.dp, (screenWidth - totalSize) / 2, expandProgress)
    val yOffset = lerp(12.dp, 100.dp, expandProgress)

    Box(
        modifier = Modifier
            .offset(x = xOffset, y = yOffset)
            .size(totalSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        val artworkUri = window.mediaItem.mediaMetadata.artworkUri
        val highResUrl = remember(artworkUri) {
            if (artworkUri.toString().isNotBlank()) {
                val url = artworkUri.thumbnail(1024).toString()
                Log.d("SharedThumbnail", "Loading High-Res URL: $url")
                url
            } else null
        }
        val imageRequest = remember(highResUrl) {
            ImageRequest.Builder(context)
                .data(highResUrl)
                .crossfade(true)
                .memoryCacheKey(highResUrl)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            placeholder = painterResource(id = R.drawable.app_icon),
            error = painterResource(id = R.drawable.app_icon),
            fallback = painterResource(id = R.drawable.app_icon),
            contentDescription = "Album Art",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}