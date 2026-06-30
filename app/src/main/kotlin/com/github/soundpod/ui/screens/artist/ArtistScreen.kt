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
    Overview, Songs, Albums, Singles
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

    val isLocal = browseId.startsWith("local_artist_")

    val tabs = remember(artistPage, isLocal) {
        if (isLocal) {
            listOf(ArtistTab.Songs to R.string.tracks)
        } else {
            listOfNotNull(
                ArtistTab.Overview to R.string.overview,
                if (artistPage?.songs != null || artistPage?.songsEndpoint != null) ArtistTab.Songs to R.string.tracks else null,
                if (artistPage?.albums != null || artistPage?.albumsEndpoint != null) ArtistTab.Albums to R.string.albums else null,
                if (artistPage?.singles != null || artistPage?.singlesEndpoint != null) ArtistTab.Singles to R.string.singles else null
            )
        }
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
            Column(
                modifier = Modifier
                    .padding(top = 22.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdaptiveThumbnail(
                    isLoading = artist?.timestamp == null,
                    url = artist?.thumbnailUrl,
                    modifier = Modifier.fillMaxWidth(0.65f)
                )
                Text(
                    text = artist?.name.orEmpty(),
                    style = typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = colorPalette.text,
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
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = playerPadding),
                    verticalAlignment = Alignment.Top
                ) { pageIndex ->
                    val tab = tabs[pageIndex].first
                    
                    when (tab) {
                        ArtistTab.Overview -> ArtistOverviewContent(
                            youtubeArtistPage = artistPage,
                            onAlbumClick = onAlbumClick,
                            playerPadding = playerPadding
                        )
                        ArtistTab.Songs -> if (isLocal) {
                            LocalArtistSongs(
                                browseId = browseId,
                                onGoToAlbum = onAlbumClick
                            )
                        } else {
                            ArtistTracksPage(
                                browseId = artistPage?.songsEndpoint?.browseId ?: browseId,
                                params = artistPage?.songsEndpoint?.params,
                                onAlbumClick = onAlbumClick,
                                onArtistClick = onArtistClick,
                                initialItems = if (artistPage?.songsEndpoint == null) artistPage?.songs else null
                            )
                        }
                        ArtistTab.Albums -> ArtistAlbumsPage(
                            browseId = artistPage?.albumsEndpoint?.browseId ?: browseId,
                            params = artistPage?.albumsEndpoint?.params,
                            onAlbumClick = onAlbumClick,
                            initialItems = if (artistPage?.albumsEndpoint == null) artistPage?.albums else null
                        )
                        ArtistTab.Singles -> ArtistAlbumsPage(
                            browseId = artistPage?.singlesEndpoint?.browseId ?: browseId,
                            params = artistPage?.singlesEndpoint?.params,
                            onAlbumClick = onAlbumClick,
                            initialItems = if (artistPage?.singlesEndpoint == null) artistPage?.singles else null
                        )
                    }
                }
            }
        }
    )
}
