package com.github.soundpod.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.core.ui.LocalAppearance
import com.github.innertube.models.NavigationEndpoint
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.appearance.LoadingAnimation
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.ShimmerHost
import com.github.soundpod.ui.items.AlbumItem
import com.github.soundpod.ui.items.ArtistItem
import com.github.soundpod.ui.items.ListItemPlaceholder
import com.github.soundpod.ui.items.LocalArtistItem
import com.github.soundpod.ui.items.SongItem
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlay
import com.github.soundpod.utils.forcePlayFromBeginning
import com.github.soundpod.viewmodels.home.FollowingViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun FollowingScreen(
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    viewModel: FollowingViewModel = viewModel()
) {
    val followedArtists = viewModel.followedArtists
    val recommendationsResult = viewModel.recommendations
    val latestReleases = viewModel.latestReleases
    val suggestedArtists = viewModel.suggestedArtists
    val popularArtists = viewModel.popularArtists

    val playerPadding = LocalPlayerPadding.current
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.loadRemix() },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            if (viewModel.isRefreshing || pullToRefreshState.distanceFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .graphicsLayer {
                            // Scale down when starting to pull, full size when reached threshold
                            val scale = pullToRefreshState.distanceFraction.coerceIn(0.5f, 1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = pullToRefreshState.distanceFraction.coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    LoadingAnimation(
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val pullOffset = pullToRefreshState.distanceFraction * 80.dp.toPx()
                    val refreshingOffset = if (viewModel.isRefreshing) 80.dp.toPx() else 0f
                    translationY = maxOf(pullOffset, refreshingOffset)
                }
        ) {
            if (followedArtists.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp + playerPadding)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = colorPalette.text.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No artists followed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorPalette.text
                                )
                                Text(
                                    text = "Follow artists to get a personalized Remix and updates",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorPalette.text.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }

                    if (popularArtists.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.you_might_also_like),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = colorPalette.text
                            )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(items = popularArtists, key = { it.key }) { artist ->
                                ArtistItem(
                                    modifier = Modifier.width(116.dp),
                                    artist = artist,
                                    onClick = { onArtistClick(artist.key) }
                                )
                            }
                        }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp + playerPadding)
                ) {
                    // Header with Followed Artists
                item {
                    Text(
                        text = stringResource(R.string.followed_artists),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorPalette.text,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                    followedArtists.chunked(4).forEach { rowArtists ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp), // Reduced slightly to account for internal padding
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowArtists.forEach { artist ->
                                    LocalArtistItem(
                                        modifier = Modifier.weight(1f),
                                        artist = artist,
                                        onClick = { onArtistClick(artist.id) }
                                    )
                                }
                                repeat(4 - rowArtists.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                    // Latest Releases Section
                    if (latestReleases.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.top_releases),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = colorPalette.text
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items = latestReleases, key = { it.key }) { album ->
                                    AlbumItem(
                                        modifier = Modifier.width(140.dp),
                                        album = album,
                                        onClick = { onAlbumClick(album.key) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Remix Section Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Following Remix",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorPalette.text
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                recommendationsResult?.getOrNull()?.let { songs ->
                                    TextButton(
                                        onClick = {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayFromBeginning(songs.map { it.asMediaItem })
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = colorPalette.accent)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Play")
                                    }

                                    TextButton(
                                        onClick = {
                                            binder?.stopRadio()
                                            binder?.player?.forcePlayFromBeginning(songs.shuffled().map { it.asMediaItem })
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = colorPalette.accent)
                                    ) {
                                        Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Shuffle")
                                    }
                                }
                            }
                        }
                    }

                    // Remix Content or Shimmer
                    recommendationsResult?.let { result ->
                        result.onSuccess { songs ->
                            if (!songs.isNullOrEmpty()) {
                                items(items = songs) { song ->
                                    SongItem(
                                        song = song,
                                        onClick = {
                                            val mediaItem = song.asMediaItem
                                            binder?.stopRadio()
                                            binder?.player?.forcePlay(mediaItem)
                                            binder?.setupRadio(
                                                NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                                            )
                                        },
                                        onLongClick = {
                                            menuState.display {
                                                NonQueuedMediaItemMenu(
                                                    onDismiss = menuState::hide,
                                                    mediaItem = song.asMediaItem,
                                                    onGoToAlbum = onAlbumClick,
                                                    onGoToArtist = onArtistClick
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text(text = "No songs found for your remix", color = colorPalette.text)
                                    }
                                }
                            }
                        }.onFailure {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(text = "Error loading remix", color = Color.Red)
                                }
                            }
                        }
                    } ?: item {
                        ShimmerHost(modifier = Modifier.padding(horizontal = 16.dp)) {
                            repeat(10) { ListItemPlaceholder() }
                        }
                    }

                    // Suggested Artists Section
                    if (suggestedArtists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.you_might_also_like),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = colorPalette.text
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(items = suggestedArtists, key = { it.key }) { artist ->
                                    ArtistItem(
                                        modifier = Modifier.width(100.dp),
                                        artist = artist,
                                        onClick = { onArtistClick(artist.key) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
