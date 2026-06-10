package com.github.soundpod.ui.screens.artist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.core.ui.LocalAppearance
import com.github.innertube.Innertube
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.AdaptiveThumbnail
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.PlaylistScreenLayout
import com.github.soundpod.ui.components.ShimmerHost
import com.github.soundpod.ui.components.TextPlaceholder
import com.github.soundpod.ui.items.AlbumItem
import com.github.soundpod.ui.items.ArtistItem
import com.github.soundpod.ui.items.ListItemPlaceholder
import com.github.soundpod.ui.items.PlaylistItem
import com.github.soundpod.ui.items.SongItem
import com.github.soundpod.ui.styling.Dimensions
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlayAtIndex
import com.github.soundpod.viewmodels.ArtistViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun NewArtistScreen(
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
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdaptiveThumbnail(
                    isLoading = artist?.timestamp == null,
                    url = artist?.thumbnailUrl,
                    modifier = Modifier.fillMaxWidth(0.55f)
                )

                Spacer(modifier = Modifier.height(8.dp))
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
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp + playerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArtistOverviewContent(
                    youtubeArtistPage = artistPage,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    onPlaylistClick = onPlaylistClick
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
private fun ArtistOverviewContent(
    youtubeArtistPage: Innertube.ArtistPage?,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    val itemSize = 140.dp

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (youtubeArtistPage != null) {
            youtubeArtistPage.songs?.let { songs ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.songs),
                        style = typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                songs.forEachIndexed { index, song ->
                    SongItem(
                        song = song,
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(songs.map { it.asMediaItem }, index)
                        },
                        onLongClick = {
                            menuState.display {
                                NonQueuedMediaItemMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = song.asMediaItem,
                                    onGoToAlbum = onAlbumClick
                                )
                            }
                        }
                    )
                }
            }

            youtubeArtistPage.albums?.let { albums ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.albums),
                        style = typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = albums.filter { it.key.isNotEmpty() }.distinctBy { it.key },
                        key = Innertube.AlbumItem::key
                    ) { album ->
                        AlbumItem(
                            modifier = Modifier.widthIn(max = itemSize),
                            album = album,
                            onClick = { onAlbumClick(album.key) }
                        )
                    }
                }
            }

            youtubeArtistPage.singles?.let { singles ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.singles),
                        style = typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = singles.filter { it.key.isNotEmpty() }.distinctBy { it.key },
                        key = Innertube.AlbumItem::key
                    ) { album ->
                        AlbumItem(
                            modifier = Modifier.widthIn(max = itemSize),
                            album = album,
                            onClick = { onAlbumClick(album.key) }
                        )
                    }
                }
            }

            youtubeArtistPage.playlists?.let { playlists ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.playlists),
                        style = typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = playlists.filter { it.key.isNotEmpty() }.distinctBy { it.key },
                        key = Innertube.PlaylistItem::key
                    ) { playlist ->
                        PlaylistItem(
                            modifier = Modifier.widthIn(max = itemSize),
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.key) }
                        )
                    }
                }
            }

            youtubeArtistPage.relatedArtists?.let { artists ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.fans_might_also_like),
                        style = typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = artists.filter { it.key.isNotEmpty() }.distinctBy { it.key },
                        key = Innertube.ArtistItem::key
                    ) { artist ->
                        ArtistItem(
                            modifier = Modifier.widthIn(max = itemSize),
                            artist = artist,
                            onClick = { onArtistClick(artist.key) }
                        )
                    }
                }
            }

            youtubeArtistPage.description?.let { description ->
                val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.about),
                        style = typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Text(
                    text = if (attributionsIndex == -1) {
                        description
                    } else {
                        description.substring(0, attributionsIndex)
                    },
                    style = typography.bodySmall,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .alpha(Dimensions.MEDIUMOPACITY)
                )

                if (attributionsIndex != -1) {
                    Text(
                        text = "From Wikipedia under Creative Commons Attribution CC-BY-SA 3.0",
                        style = typography.bodySmall,
                        modifier = Modifier
                            .alpha(Dimensions.MEDIUMOPACITY)
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    )
                }
            }
        } else {
            ShimmerHost(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                val placeholderModifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)

                TextPlaceholder(modifier = placeholderModifier)

                repeat(5) {
                    ListItemPlaceholder()
                }
            }
        }
    }
}
