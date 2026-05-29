@file:OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)

package com.github.soundpod.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.ui.components.HorizontalTabs
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.navigation.Routes

@Composable
fun HomeScreen(
    navController: NavController,
    onSettingsClick: () -> Unit
) {


    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
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
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
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
                    contentDescription = "Search",
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
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = colorPalette.text
                )
            }
        }
    ) {
        HorizontalTabs(pagerState = pagerState)

        SettingsCard{
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> QuickPicks(
                        onAlbumClick = navigateToAlbum,
                        onArtistClick = navigateToArtist,
                        onPlaylistClick = { browseId ->
                            navController.navigate(route = Routes.Playlist(id = browseId))
                        },
                        onOfflinePlaylistClick = {
                            navController.navigate(route = Routes.BuiltInPlaylist(index = 1))
                        }
                    )

                    1 -> HomeSongs(
                        onGoToAlbum = navigateToAlbum,
                        onGoToArtist = navigateToArtist
                    )

                    2 -> HomeArtistList(
                        onArtistClick = { artist -> navigateToArtist(artist.id) }
                    )

                    3 -> HomeAlbums(
                        onAlbumClick = { album -> navigateToAlbum(album.id) }
                    )

                    4 -> HomePlaylists(
                        onBuiltInPlaylist = { playlistIndex ->
                            navController.navigate(route = Routes.BuiltInPlaylist(index = playlistIndex))
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