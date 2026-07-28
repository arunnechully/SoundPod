@file:OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)

package com.github.soundpod.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.soundpod.ui.components.ClassicHorizontalTabs
import com.github.soundpod.ui.components.HorizontalTabs
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.ui.navigation.Routes
import com.github.soundpod.ui.screens.favorites.FavoritesScreen
import com.github.soundpod.utils.HomeTab
import com.github.soundpod.utils.TabStyle
import com.github.soundpod.utils.appearanceUpdatedKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.tabStyleKey
import com.github.soundpod.utils.updateAvailableKey
import com.github.soundpod.viewmodels.home.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    onSettingsClick: () -> Unit,
) {
    val tabStyle by rememberPreference(tabStyleKey, TabStyle.Modern)
    val updateAvailable by rememberPreference(updateAvailableKey, false)
    val appearanceUpdated by rememberPreference(appearanceUpdatedKey, false)

    val showSettingsBadge = updateAvailable || appearanceUpdated

    val activeTabs = HomeTab.entries.filter { tab ->
        rememberPreference(tab.key, true).value
    }.ifEmpty { listOf(HomeTab.Home) }

    val pagerState = rememberPagerState(initialPage = 0) { activeTabs.size }
    val navigateToAlbum = { browseId: String ->
        navController.navigate(route = Routes.Album(id = browseId))
    }
    val navigateToArtist = { browseId: String ->
        navController.navigate(route = Routes.Artist(id = browseId))
    }

    val (colorPalette) = LocalAppearance.current

    SettingsScreenLayout(
        title = {
            Text(
                text = "SoundPod",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorPalette.text
            )
        },
        scrollable = false,
        horizontalPadding = 0.dp,
        actions = {
            OutlinedButton(
                onClick = { navController.navigate(route = Routes.Search) },
                shape = RoundedCornerShape(60),
                border = BorderStroke(1.dp, Color.Gray),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = colorPalette.text
                )

                Text(
                    text = stringResource(R.string.search),
                    color = colorPalette.text,
                    style = typography.bodyMedium
                )
            }
            OutlinedIconButton(
                onClick = onSettingsClick,
                border = BorderStroke(1.dp, Color.Gray)
            ) {
                BadgedBox(
                    badge = {
                        if (showSettingsBadge) {
                            Badge(
                                modifier = Modifier.size(8.dp),
                                containerColor = Color.Red
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = colorPalette.text
                    )
                }
            }
        }
    ) {
        val tabTitles = activeTabs.map { it.title }

        if (tabStyle == TabStyle.Modern) {
            HorizontalTabs(
                pagerState = pagerState,
                tabs = tabTitles
            )
        } else {
            ClassicHorizontalTabs(
                pagerState = pagerState,
                tabs = tabTitles
            )
        }

        SettingsCard(
            modifier = Modifier.weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 4,
                modifier = Modifier
                    .fillMaxSize()
            ) { page ->
                when (activeTabs[page]) {
                    HomeTab.Home -> QuickPicks(
                        onAlbumClick = navigateToAlbum,
                        onArtistClick = navigateToArtist,
                        onPlaylistClick = { browseId ->
                            navController.navigate(route = Routes.Playlist(id = browseId))
                        },
                        onOfflinePlaylistClick = {
                            navController.navigate(route = Routes.BuiltInPlaylist(index = 1))
                        }
                    )

                    HomeTab.Favorites -> FavoritesScreen(
                        onFavoriteTracksClick = { navController.navigate(route = Routes.FavoriteTracks) },
                        onGoToAlbum = navigateToAlbum,
                        onGoToArtist = navigateToArtist,
                        isEmbedded = true
                    )

                    HomeTab.Songs -> HomeSongs(
                        onGoToAlbum = navigateToAlbum,
                        onGoToArtist = navigateToArtist
                    )

                    HomeTab.Artists -> HomeArtistList(
                        onArtistClick = { artist -> navigateToArtist(artist.id) }
                    )

                    HomeTab.Albums -> HomeAlbums(
                        onAlbumClick = { album -> navigateToAlbum(album.id) }
                    )

                    HomeTab.Playlists -> HomePlaylists(
                        onBuiltInPlaylist = { playlistIndex ->
                            if (playlistIndex == BuiltInPlaylist.Favorites.ordinal) {
                                navController.navigate(route = Routes.Favorites)
                            } else {
                                navController.navigate(route = Routes.BuiltInPlaylist(index = playlistIndex))
                            }
                        },
                        onPlaylistClick = { playlist ->
                            navController.navigate(route = Routes.LocalPlaylist(id = playlist.id))
                        }
                    )
                }
            }
        }
    }

}