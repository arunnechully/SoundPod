package com.github.soundpod.ui.screens.player.layout.main

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.ContentMetadata
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.ProgressBar
import com.github.soundpod.query
import com.github.soundpod.ui.screens.player.ActionChipItem
import com.github.soundpod.ui.screens.player.PlayerTabs
import com.github.soundpod.ui.screens.player.PlayerMediaItem
import com.github.soundpod.ui.screens.player.PlayerSeekBar
import com.github.soundpod.ui.screens.player.PlayerTopControl
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.PlayerLayout
import com.github.soundpod.utils.forceSeekToNext
import com.github.soundpod.utils.forceSeekToPrevious
import com.github.soundpod.utils.progressBarStyle
import com.github.soundpod.utils.queueLoopEnabledKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.shuffleQueue
import com.github.soundpod.utils.trackLoopEnabledKey
import com.github.soundpod.viewmodels.PlayerViewModel
import com.github.soundpod.viewmodels.PlaylistViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

@OptIn(UnstableApi::class)
@Composable
fun YoutubePlayer(
    expandProgress: Float,
    layoutMode: PlayerLayout,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onTrackDetailsClick: () -> Unit = {},
    onBack: () -> Unit,
    showPlaylist: Boolean,
    onSettingsClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onLyricsClick: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onTogglePlaylist: (Boolean) -> Unit,
    showLyrics: Boolean
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return

    val (colorPalette) = LocalAppearance.current

    val playlistViewModel = remember(player) { PlaylistViewModel(player) }
    val playerViewModel = remember(player) { PlayerViewModel(player) }

    val uiState by playerViewModel.uiState.collectAsState()

    val mediaItem = uiState.mediaItem
    val artistId = uiState.artistId
    val isSingleArtist = uiState.isSingleArtist
    val albumId = uiState.albumId
    val shouldBePlaying =
        uiState.playWhenReady && uiState.playbackState != Player.STATE_ENDED
    val currentPositionMs = uiState.currentPositionMs
    val durationMs = uiState.durationMs

    var trackLoopEnabled by rememberPreference(trackLoopEnabledKey, defaultValue = false)
    var queueLoopEnabled by rememberPreference(queueLoopEnabledKey, defaultValue = false)

    val handleGoToAlbum: (String) -> Unit = remember(onGoToAlbum, onBack) {
        { id -> onBack(); onGoToAlbum(id) }
    }

    val handleGoToArtist: (String) -> Unit = remember(onGoToArtist, onBack) {
        { id -> onBack(); onGoToArtist(id) }
    }

    BackHandler(enabled = true) {
        if (showPlaylist) onTogglePlaylist(false) else onBack()
    }

    var isDraggingSeekBar by remember { mutableStateOf(false) }

    var isFullyCached by remember { mutableStateOf(false) }
    LaunchedEffect(mediaItem?.mediaId, binder) {
        val videoId = mediaItem?.mediaId ?: return@LaunchedEffect
        val cache = binder.cache

        combine(
            db.isDownloaded(videoId),
            db.format(videoId),
            binder.cacheChanges.onStart { emit(Unit) }
        ) { downloaded, format, _ ->
            if (downloaded) {
                val metadata = cache.getContentMetadata(videoId)
                val length = ContentMetadata.getContentLength(metadata).takeIf { it > 0 }
                    ?: format?.contentLength ?: -1L

                if (length > 0) {
                    cache.isCached(videoId, 0, length)
                } else {
                    cache.getCachedBytes(videoId, 0, -1) > 0L
                }
            } else {
                false
            }
        }.distinctUntilChanged().collect {
            isFullyCached = it
        }
    }

    val progressBarStyleState = rememberPreference(progressBarStyle, ProgressBar.Default)
    val currentProgressStyle = progressBarStyleState.value

    val playingScale by animateFloatAsState(
        targetValue = if (shouldBePlaying) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "playingScale"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidth = maxWidth
        val thumbnailSize = containerWidth * 0.85f

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 48.dp
                )
        ) {
            Spacer(modifier = Modifier.height(Dimensions.spacer))

//                Top Control
            PlayerTopControl(
                onGoToAlbum = albumId?.let { id -> { handleGoToAlbum(id) } },
                onGoToArtist = if (isSingleArtist && artistId != null) {
                    { handleGoToArtist(artistId) }
                } else null,
                onTrackDetailsClick = onTrackDetailsClick,
                onLyricsClick = onLyricsClick,
                onSettingsClick = onSettingsClick,
                onSleepTimerClick = onSleepTimerClick,
                onBack = {
                    if (showPlaylist) onTogglePlaylist(false) else onBack()
                },
                isPlaylistShowing = if (layoutMode == PlayerLayout.SamsungMusic) showPlaylist || showLyrics else showPlaylist,
                isFullyCached = isFullyCached
            )

            Box(Modifier.weight(1f)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    PlayerMediaItem(
                        onGoToArtist = if (isSingleArtist && artistId != null) {
                            { handleGoToArtist(artistId) }
                        } else null,
                        contentAlignment = Alignment.Start
                    )

                    Spacer(modifier = Modifier.height(Dimensions.spacer))

                    val mediaId = mediaItem?.mediaId

                    var likedAt by remember(mediaId) { mutableStateOf<Long?>(null) }
                    var blacklistedAt by remember(mediaId) { mutableStateOf<Long?>(null) }

                    LaunchedEffect(mediaId) {
                        if (mediaId != null) {
                            db.likedAt(mediaId).distinctUntilChanged().collect { likedAt = it }
                        } else {
                            likedAt = null
                        }
                    }

                    LaunchedEffect(mediaId) {
                        if (mediaId != null) {
                            db.blacklistedAt(mediaId).distinctUntilChanged().collect { blacklistedAt = it }
                        } else {
                            blacklistedAt = null
                        }
                    }

                    val isLiked = likedAt != null
                    val isDisliked = blacklistedAt != null

                    PlayerTabs(
                        isLiked = isLiked,
                        isDisliked = isDisliked,
                        onLikeClick = {
                            val currentMediaItem = binder?.player?.currentMediaItem ?: return@PlayerTabs
                            val id = currentMediaItem.mediaId
                            query {
                                val newLikedAt = if (likedAt == null) System.currentTimeMillis() else null
                                if (db.like(id, newLikedAt) == 0) {
                                    db.insert(currentMediaItem) { song ->
                                        song.copy(likedAt = newLikedAt, blacklistedAt = null)
                                    }
                                }
                            }
                        },
                        onDislikeClick = {
                            val currentMediaItem = binder?.player?.currentMediaItem ?: return@PlayerTabs
                            val id = currentMediaItem.mediaId
                            val isCurrentlyBlacklisted = blacklistedAt != null
                            query {
                                val newBlacklistedAt = if (isCurrentlyBlacklisted) null else System.currentTimeMillis()
                                if (db.blacklist(id, newBlacklistedAt) == 0) {
                                    db.insert(currentMediaItem) { song ->
                                        song.copy(blacklistedAt = newBlacklistedAt, likedAt = null)
                                    }
                                }
                            }
                            if (!isCurrentlyBlacklisted) {
                                val player = binder?.player
                                val currentIndex = player?.currentMediaItemIndex
                                if (player != null && currentIndex != null && currentIndex >= 0) {
                                    player.removeMediaItem(currentIndex)
                                }
                            }
                        },
                        likeIcon = R.drawable.like,
                        likeFilledIcon = R.drawable.like_filled,
                        dislikeIcon = R.drawable.dislike,
                        dislikeFilledIcon = R.drawable.dislike_filled,
                        pillBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        contentColor = colorPalette.text,
                        extraActions = listOf(
                            ActionChipItem(
                                id = "Playlist",
                                icon = R.drawable.save_playlist,
                                label = "Save",
                                onClick = { onAddToPlaylist() }
                            ),
                            ActionChipItem(
                                id = "share",
                                icon = R.drawable.share,
                                label = "Share",
                                onClick = { /* share track */ }
                            ),
                            ActionChipItem(
                                id = "download",
                                icon = R.drawable.download,
                                label = "Download",
                                onClick = { /* share track */ }
                            ),
                            ActionChipItem(
                                id = "lyrics",
                                icon = R.drawable.quote,
                                label = "Lyrics",
                                onClick = { onLyricsClick() }
                            )
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spacer))

            PlayerSeekBar(
                mediaId = mediaItem?.mediaId ?: "",
                position = currentPositionMs,
                duration = durationMs,
                progressBarStyle = currentProgressStyle,
                isPlaying = shouldBePlaying,
                onDraggingStateChange = { isDraggingSeekBar = it }
            )

            Spacer(modifier = Modifier.height(Dimensions.spacer))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                // Shuffle
                IconButton(
                    onClick = {
                        player.shuffleModeEnabled = !player.shuffleModeEnabled
                        player.shuffleQueue()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = colorPalette.iconColor,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = { player.forceSeekToPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.play_skip_back),
                        contentDescription = "Previous",
                        tint = colorPalette.iconColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(
                    onClick = { playerViewModel.togglePlayPause() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colorPalette.iconColor)
                ) {
                    Icon(
                        painter = painterResource(id = if (shouldBePlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = colorPalette.background4.copy(alpha = 1f),
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = { player.forceSeekToNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.play_skip_forward),
                        contentDescription = "Next",
                        tint = colorPalette.iconColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Repeat
                IconButton(
                    onClick = {
                        if (trackLoopEnabled) {
                            trackLoopEnabled = false
                        } else if (queueLoopEnabled) {
                            queueLoopEnabled = false
                            trackLoopEnabled = true
                        } else {
                            queueLoopEnabled = true
                        }
                    },
                    modifier = Modifier.size(48.dp)
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
                    Icon(
                        painter = icon,
                        tint = colorPalette.iconColor,
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                    )
                }
            }
        }
    }
}