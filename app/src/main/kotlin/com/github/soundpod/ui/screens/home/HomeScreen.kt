@file:OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)

package com.github.soundpod.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.api.GitHub
import com.github.api.formatFileSize
import com.github.core.ui.LocalAppearance
import com.github.soundpod.ui.common.autoCheckEnabled
import com.github.soundpod.ui.common.showUpdateAlert
import com.github.soundpod.ui.components.HorizontalTabs
import com.github.soundpod.ui.components.TopBar
import com.github.soundpod.ui.navigation.Routes
import com.github.soundpod.ui.screens.settings.newsettings.extractVersion
import com.github.soundpod.ui.screens.settings.newsettings.isNewerVersion
import com.github.soundpod.utils.downloadApk

@Composable
fun HomeScreen(
    navController: NavController
) {


    val context = LocalContext.current

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })

    val (colorPalette) = LocalAppearance.current

    val navigateToAlbum = { browseId: String ->
        navController.navigate(route = Routes.Album(id = browseId))
    }

    val navigateToArtist = { browseId: String ->
        navController.navigate(route = Routes.Artist(id = browseId))
    }

    var autoCheckEnabled by remember { mutableStateOf(true) }
    var showAlertEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        autoCheckEnabled(context).collect { autoCheckEnabled = it }
    }

    LaunchedEffect(Unit) {
        showUpdateAlert(context).collect { showAlertEnabled = it }
    }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var apkAsset by remember { mutableStateOf<com.github.api.Asset?>(null) }
    var apkUrl by remember { mutableStateOf<String?>(null) }
    var latestVersion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(autoCheckEnabled, showAlertEnabled) {
        if (!autoCheckEnabled) return@LaunchedEffect

        val release = GitHub.getLastestRelease()
        val asset = release?.assets?.firstOrNull { it.name.endsWith(".apk") }
        val latest = release?.name?.let { extractVersion(it) }
        val current = context.packageManager
            .getPackageInfo(context.packageName, 0).versionName ?: "0"

        val isNewer = latest != null && isNewerVersion(latest, current)

        if (isNewer && showAlertEnabled) {
            apkAsset = asset
            apkUrl = asset?.browserDownloadUrl
            latestVersion = latest
            showUpdateDialog = true
        }
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
                .background(color = colorPalette.baseColor)
        ) {
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

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            confirmButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Close")
                }
            },
            title = { Text("New version available") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    latestVersion?.let {
                        Text("Version: $it", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(Modifier.size(8.dp))

                    apkAsset?.let { asset ->
                        Text("File Size: ${formatFileSize(asset.size)}")
                    }

                    Spacer(Modifier.size(12.dp))

                    FilledTonalButton(
                        onClick = {
                            apkUrl?.let { url ->
                                downloadApk(context, url)
                            }
                        }
                    ) {
                        Text("Update Now")
                    }
                }
            }
        )
    }

}