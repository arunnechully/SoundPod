package com.github.soundpod.ui.screens.player

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBarDefaults.windowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.soundpod.Database
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.ui.components.SeekBar
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.DisposableListener
import com.github.soundpod.utils.isLandscape
import com.github.soundpod.utils.positionAndDurationState
import com.github.soundpod.utils.shouldBePlaying
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun NewPlayer(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return
    var nullableMediaItem by remember {
        mutableStateOf(
            binder.player.currentMediaItem,
            neverEqualPolicy()
        )
    }

    val mediaItem = nullableMediaItem ?: return

    var artistId: String? by remember(mediaItem) {
        mutableStateOf(
            mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { artists ->
                if (artists.size == 1) artists.first()
                else null
            }
        )
    }
    var isShowingLyrics by rememberSaveable { mutableStateOf(false) }
    var fullScreenLyrics by remember { mutableStateOf(false) }
    var isShowingStatsForNerds by rememberSaveable { mutableStateOf(false) }

    var shouldBePlaying by remember { mutableStateOf(binder.player.shouldBePlaying) }

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

    LaunchedEffect(mediaItem) {
        withContext(Dispatchers.IO) {
            if (artistId == null) {
                val artistsInfo = Database.songArtistInfo(mediaItem.mediaId)
                if (artistsInfo.size == 1) artistId = artistsInfo.first().id
            }
        }
    }
    val thumbnailContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
//        Thumbnail(
//            isShowingLyrics = isShowingLyrics,
//            onShowLyrics = { isShowingLyrics = it },
//            fullScreenLyrics = fullScreenLyrics,
//            toggleFullScreenLyrics = { fullScreenLyrics = !fullScreenLyrics },
//            isShowingStatsForNerds = isShowingStatsForNerds,
//            onShowStatsForNerds = { isShowingStatsForNerds = it },
//            modifier = modifier
//        )
    }

    val controlsContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
//        Controls(
//            mediaId = mediaItem.mediaId,
//            title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
//            artist = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
//            shouldBePlaying = shouldBePlaying,
//            position = positionAndDuration.first,
//            duration = positionAndDuration.second,
//            onGoToArtist = artistId?.let {
//                { onGoToArtist(it) }
//            },
//            modifier = modifier
//        )
    }
    var showPlaylist by remember { mutableStateOf(false) }

    val mediaId = mediaItem.mediaId
    val positionAndDuration by binder.player.positionAndDurationState()
    var scrubbingPosition by remember(mediaId) { mutableStateOf<Long?>(null) }
    val position = positionAndDuration.first
    val duration = positionAndDuration.second

    var likedAt by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.weight(1F)
        ) {
            if (isLandscape) {
                //todo
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            windowInsets
                                .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                                .asPaddingValues()
                        )
                        .padding(bottom = 60.dp)
                ) {

                    Spacer(modifier = Modifier.height(Dimensions.spacer))

                    PlayerTopControl(
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist
                    )

                    PlayerMediaItem(
                        onGoToArtist = artistId?.let {
                            { onGoToArtist(it) }
                        }
                    )

                    PlayerMiddleControl(
                        likedAt = likedAt,
                        setLikedAt = {},
                        showPlaylist = showPlaylist,
                        onTogglePlaylist = { showPlaylist = it },
                        mediaId = mediaItem.mediaId
                    )

                    SeekBar(
                        value = scrubbingPosition ?: position,
                        minimumValue = 0,
                        maximumValue = duration,
                        onDragStart = {
                            scrubbingPosition = it
                        },
                        onDrag = { delta ->
                            scrubbingPosition = if (duration != C.TIME_UNSET) {
                                scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                            } else {
                                null
                            }
                        },
                        onDragEnd = {
                            scrubbingPosition?.let(binder.player::seekTo)
                            scrubbingPosition = null
                        },
                        color = MaterialTheme.colorScheme.primary,
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    )

                    PlayerControlBottom(
                        shouldBePlaying = shouldBePlaying,
                        onPlayPauseClick = {
                            if (shouldBePlaying) {
                                binder.player.pause()
                            } else {
                                if (binder.player.playbackState == Player.STATE_IDLE) {
                                    binder.player.prepare()
                                } else if (binder.player.playbackState == Player.STATE_ENDED) {
                                    binder.player.seekToDefaultPosition(0)
                                }
                                binder.player.play()
                            }
                        }
                    )

                }
            }
        }
    }
}