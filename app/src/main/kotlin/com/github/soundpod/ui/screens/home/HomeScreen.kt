@file:OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)

package com.github.soundpod.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.soundpod.ui.components.HorizontalTabs
import com.github.soundpod.ui.components.TopBar
import com.github.soundpod.ui.navigation.Routes

@Composable
fun HomeScreen(
    navController: NavController
) {
    val isDarkTheme = isSystemInDarkTheme()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 10 })
    val background = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

    val navigateToAlbum = { browseId: String ->
        navController.navigate(route = Routes.Album(id = browseId))
    }

    val navigateToArtist = { browseId: String ->
        navController.navigate(route = Routes.Artist(id = browseId))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopBar(
            onSearch = { navController.navigate(route = Routes.Search) },
            onSettingsClick = { navController.navigate(route = Routes.Settings) },
        )

        Spacer(modifier = Modifier.padding(vertical = 2.dp))

        HorizontalTabs(pagerState = pagerState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .background(color = background)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> QuickPicks(
//                        openSearch = { navController.navigate(route = Routes.Search) },
//                        openSettings = { navController.navigate(route = Routes.Settings) },
                        onAlbumClick = navigateToAlbum,
                        onArtistClick = navigateToArtist,
                        onPlaylistClick = { browseId ->
                            navController.navigate(route = Routes.Playlist(id = browseId))
                        },
                        onOfflinePlaylistClick = {
                            navController.navigate(route = Routes.BuiltInPlaylist(index = 1))
                        }
                    )
                    1 -> {}
                    2 -> {}
                    3 -> {}
                    4 -> {}
                }
            }
        }
    }
}