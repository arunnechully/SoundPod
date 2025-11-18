package com.github.soundpod.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Left
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Right
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import com.github.core.ui.LocalAppearance
import com.github.core.ui.favoritesIcon
import com.github.innertube.Innertube
import com.github.innertube.requests.visitorData
import com.github.soundpod.Database
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.Song
import com.github.soundpod.query
import com.github.soundpod.service.LoginRequiredException
import com.github.soundpod.service.PlayableFormatNotFoundException
import com.github.soundpod.service.UnplayableException
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.ui.styling.px
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.currentWindow
import com.github.soundpod.utils.forceSeekToNext
import com.github.soundpod.utils.forceSeekToPrevious
import com.github.soundpod.utils.thumbnail
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

@Composable
fun NewThumbnail(
    mediaId: String,
    isShowingLyrics: Boolean,
    onShowLyrics: (Boolean) -> Unit,
    fullScreenLyrics: Boolean,
    toggleFullScreenLyrics: () -> Unit,
    isShowingStatsForNerds: Boolean,
    onShowStatsForNerds: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    var nullableWindow by remember { mutableStateOf(player.currentWindow) }

    var error by remember { mutableStateOf<PlaybackException?>(player.playerError) }
    var errorCounter by remember(error) { mutableIntStateOf(0) }

    val (thumbnailSizeDp, thumbnailSizePx) = Dimensions.thumbnails.player.song.let {
        it to (it - 64.dp).px
    }
    var likedAt by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(mediaId) {
        Database.likedAt(mediaId).distinctUntilChanged().collect { likedAt = it }
    }

    val retry = {
        when (error?.cause?.cause) {
            is UnresolvedAddressException,
            is UnknownHostException,
            is PlayableFormatNotFoundException,
            is UnplayableException,
            is LoginRequiredException -> player.prepare()

            else -> {
                runBlocking {
                    Innertube.visitorData = Innertube.visitorData().getOrNull()
                }
                player.prepare()
            }
        }
    }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableWindow = player.currentWindow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                error = player.playerError
            }

            @androidx.annotation.OptIn(UnstableApi::class)
            override fun onPlayerError(playbackException: PlaybackException) {
                error = playbackException

                if (errorCounter == 0) {
                    retry()
                }
            }
        }
    }

    val window = nullableWindow ?: return

    val coroutineScope = rememberCoroutineScope()
    val transitionState = remember { SeekableTransitionState(false) }
    val transition = rememberTransition(transitionState)
    val opacity by transition.animateFloat(label = "") { if (it) 1f else 0f }
    val scale by transition.animateFloat(
        label = "",
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioLowBouncy)
        }
    ) { if (it) 1f else 0f }

    AnimatedContent(
        targetState = window,
        transitionSpec = {
            val duration = 500
            val initial = initialState
            val target = targetState

            if (initial == null || target == null) return@AnimatedContent ContentTransform(
                targetContentEnter = fadeIn(tween(duration)),
                initialContentExit = fadeOut(tween(duration)),
                sizeTransform = null
            )

            val sizeTransform = SizeTransform(clip = false) { _, _ ->
                tween(durationMillis = duration, delayMillis = duration)
            }

            val direction = if (target.firstPeriodIndex < initial.firstPeriodIndex) Right else Left

            ContentTransform(
                targetContentEnter = slideIntoContainer(direction, tween(duration)) +
                        fadeIn(tween(duration)) +
                        scaleIn(tween(duration), 0.85f),
                initialContentExit = slideOutOfContainer(direction, tween(duration)) +
                        fadeOut(tween(duration)) +
                        scaleOut(tween(duration), 0.85f),
                sizeTransform = sizeTransform
            )
        },
        modifier = modifier.onSwipe(
            onSwipeLeft = {
                binder.player.forceSeekToNext()
            },
            onSwipeRight = {
                binder.player.forceSeekToPrevious()
            }
        ),
        contentAlignment = Alignment.Center,
        label = ""
    ) { currentWindow ->
        val blurRadius by animateDpAsState(
            targetValue = if (isShowingLyrics || error != null || isShowingStatsForNerds) 8.dp else 0.dp,
            animationSpec = tween(500),
            label = ""
        )

        val (colorPalette, _) = LocalAppearance.current
        val isDarkTheme = colorPalette.background2.luminance() < 0.5f

        val glassColor = if (isDarkTheme) {
            Color.White.copy(alpha = 0.07f)
        } else {
            Color.Black.copy(alpha = 0.04f)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(glassColor)
        ) {

            var height by remember { mutableIntStateOf(0) }
            val artworkUri = currentWindow.mediaItem.mediaMetadata.artworkUri
            val model = if (artworkUri.toString().isNotBlank()) {
                artworkUri.thumbnail((Dimensions.thumbnails.player.song - 54.dp).px)
            } else null

            AsyncImage(
                model = model,
                placeholder = painterResource(id = R.drawable.app_icon),
                error = painterResource(id = R.drawable.app_icon),
                fallback = painterResource(id = R.drawable.app_icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onShowLyrics(true) },
                            onLongPress = { onShowStatsForNerds(true) },
                            onDoubleTap = {
                                val currentMediaItem = binder.player.currentMediaItem
                                query {
                                    if (Database.like(
                                            mediaId,
                                            if (likedAt == null) System.currentTimeMillis() else null
                                        ) == 0
                                    ) {
                                        currentMediaItem
                                            ?.takeIf { it.mediaId == mediaId }
                                            ?.let {
                                                Database.insert(currentMediaItem, Song::toggleLike)
                                            }
                                    }
                                }

                                coroutineScope.launch {
                                    val spec = tween<Float>(durationMillis = 500)
                                    transitionState.animateTo(true, spec)
                                    transitionState.animateTo(false, spec)
                                }
                            }
                        )
                    }
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .let {
                        if (blurRadius == 0.dp) it else it.blur(radius = blurRadius)
                    }
                    .animateContentSize()
                    .onGloballyPositioned { coords ->
                        coords.size.height.let { if (it > 0) height = it }
                    }
            )

            Box(
                modifier = Modifier
                    .matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                Lyrics(
                    mediaId = currentWindow.mediaItem.mediaId,
                    isDisplayed = isShowingLyrics && error == null,
                    onDismiss = {
                        onShowLyrics(false)
                        if (fullScreenLyrics) toggleFullScreenLyrics()
                    },
                    ensureSongInserted = { Database.insert(currentWindow.mediaItem) },
                    size = thumbnailSizeDp,
                    mediaMetadataProvider = currentWindow.mediaItem::mediaMetadata,
                    durationProvider = player::getDuration,
                    fullScreenLyrics = fullScreenLyrics,
                    toggleFullScreenLyrics = toggleFullScreenLyrics
                )



                if (isShowingStatsForNerds) {
                    NewStatsForNerds(
                        mediaId = currentWindow.mediaItem.mediaId,
                        onDismiss = { onShowStatsForNerds(false) }
                    )
                }
            }
            Image(
                painter = painterResource(R.drawable.heart),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.favoritesIcon),
                modifier = Modifier
                    .fillMaxSize(0.5f)
                    .aspectRatio(1f)
                    .align(Alignment.Center)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = opacity,
                        shadowElevation = 8.dp.px.toFloat()
                    )
            )

        }
    }
}