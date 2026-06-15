package com.github.soundpod.ui.screens.artist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.R
import com.github.soundpod.ui.components.AdaptiveThumbnail
import com.github.soundpod.ui.components.PlaylistScreenLayout
import com.github.soundpod.viewmodels.ArtistViewModel
import kotlinx.coroutines.launch

enum class ArtistTab {
    Songs, Albums, Singles, Playlists, Related
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun ArtistScreen(
    browseId: String,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    viewModel: ArtistViewModel = viewModel(),
) {
    val playerPadding = LocalPlayerPadding.current
    val (colorPalette) = LocalAppearance.current
    val artist = viewModel.artist
    val artistPage = viewModel.artistPage

    BackHandler { onBack() }

    LaunchedEffect(browseId) {
        viewModel.loadArtist(browseId, 0)
    }

    val tabs = remember(artistPage) {
        listOfNotNull(
            if (artistPage?.songs != null) ArtistTab.Songs to R.string.tracks else null,
            if (artistPage?.albums != null) ArtistTab.Albums to R.string.albums else null,
            if (artistPage?.singles != null) ArtistTab.Singles to R.string.singles else null,
            if (artistPage?.playlists != null) ArtistTab.Playlists to R.string.playlists else null,
            if (artistPage?.relatedArtists != null) ArtistTab.Related to R.string.fans_might_also_like else null
        )
    }
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    PlaylistScreenLayout(
        title = {
            Text(
                text = artist?.name.orEmpty(),
                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colorPalette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        onBackClick = onBack,
        actions = {
            IconButton(onClick = { viewModel.toggleBookmark() }) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (artist?.bookmarkedAt != null) R.drawable.heart else R.drawable.heart_outline
                    ),
                    contentDescription = if (artist?.bookmarkedAt != null) "Unbookmark" else "Bookmark",
                    tint = if (artist?.bookmarkedAt != null) colorPalette.accent else colorPalette.text,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colorPalette.text
                )
            }
        },
        dropDownMenuContent = { dismissMenu ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    onSettingsClick()
                    dismissMenu()
                }
            )
        },
        headerContent = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AdaptiveThumbnail(
                    isLoading = artist?.timestamp == null,
                    url = artist?.thumbnailUrl,
                    modifier = Modifier.fillMaxWidth(0.65f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = artist?.name.orEmpty(),
                    style = typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = colorPalette.accent,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        },
        footerHeaderContent = {
            if (tabs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, (_, titleRes) ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) colorPalette.accent else colorPalette.boxColor.copy(alpha = 0.5f))
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(titleRes),
                                style = typography.labelMedium,
                                color = if (selected) colorPalette.background1 else colorPalette.text
                            )
                        }
                    }
                }
            }
        },
        content = {
            if (tabs.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) { pageIndex ->
                    val tab = tabs[pageIndex].first
                    
                    when (tab) {
                        ArtistTab.Songs -> ArtistTracksPage(
                            browseId = artistPage?.songsEndpoint?.browseId ?: browseId,
                            params = artistPage?.songsEndpoint?.params,
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            playerPadding = playerPadding
                        )
                        ArtistTab.Albums -> ArtistAlbumsPage(
                            browseId = artistPage?.albumsEndpoint?.browseId ?: browseId,
                            params = artistPage?.albumsEndpoint?.params,
                            onAlbumClick = onAlbumClick,
                            playerPadding = playerPadding
                        )
                        ArtistTab.Singles -> ArtistAlbumsPage(
                            browseId = artistPage?.singlesEndpoint?.browseId ?: browseId,
                            params = artistPage?.singlesEndpoint?.params,
                            onAlbumClick = onAlbumClick,
                            playerPadding = playerPadding
                        )
                        ArtistTab.Playlists -> ArtistPlaylistsPage(
                            playlists = artistPage?.playlists ?: emptyList(),
                            onPlaylistClick = onPlaylistClick,
                            playerPadding = playerPadding
                        )
                        ArtistTab.Related -> ArtistRelatedArtistsPage(
                            artists = artistPage?.relatedArtists ?: emptyList(),
                            onArtistClick = onArtistClick,
                            playerPadding = playerPadding
                        )
                    }
                }
            } else if (artistPage == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp + playerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ArtistOverviewContent(
                        youtubeArtistPage = null,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaylistClick = onPlaylistClick,
                    )
                }
            }
        }
    )
}
