package com.github.soundpod.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.enums.SettingsSection
import com.github.soundpod.ui.screens.album.AlbumScreen
import com.github.soundpod.ui.screens.artist.ArtistScreen
import com.github.soundpod.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import com.github.soundpod.ui.screens.home.HomeAlbums
import com.github.soundpod.ui.screens.home.HomeArtistList
import com.github.soundpod.ui.screens.home.HomePlaylists
import com.github.soundpod.ui.screens.home.HomeScreen
import com.github.soundpod.ui.screens.home.HomeSongs
import com.github.soundpod.ui.screens.home.QuickPicks
import com.github.soundpod.ui.screens.localplaylist.LocalPlaylistScreen
import com.github.soundpod.ui.screens.playlist.PlaylistScreen
import com.github.soundpod.ui.screens.search.SearchScreen
import com.github.soundpod.ui.screens.settings.SettingsPage
import com.github.soundpod.ui.screens.settings.SettingsScreen
import com.github.soundpod.utils.homeScreenTabIndexKey
import com.github.soundpod.utils.rememberPreference
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.rememberSlideDistance
import kotlin.reflect.KClass

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun Navigation(
    navController: NavHostController,
    sheetState: SheetState
) {
    val scope = rememberCoroutineScope()
    val slideDistance = rememberSlideDistance()
    val (screenIndex, _) = rememberPreference(homeScreenTabIndexKey, defaultValue = 0)

    NavHost(
        navController = navController,
        startDestination = TopDestinations.routes.getOrElse(
            index = screenIndex,
            defaultValue = { Routes.NewHome }
        )::class,
        enterTransition = {
            NavigationTransitions.enterTransition(
                targetDestination = targetState.destination,
                slideDistance = slideDistance
            )
        },
        exitTransition = {
            NavigationTransitions.exitTransition(
                targetDestination = targetState.destination,
                slideDistance = slideDistance
            )
        },
        popEnterTransition = {
            NavigationTransitions.popEnterTransition(
                initialDestination = initialState.destination,
                targetDestination = targetState.destination,
                slideDistance = slideDistance
            )
        },
        popExitTransition = {
            NavigationTransitions.popExitTransition(
                initialDestination = initialState.destination,
                targetDestination = targetState.destination,
                slideDistance = slideDistance
            )
        }
    ) {
        val navigateToAlbum = { browseId: String ->
            navController.navigate(route = Routes.Album(id = browseId))
        }

        val navigateToArtist = { browseId: String ->
            navController.navigate(route = Routes.Artist(id = browseId))
        }

        val popDestination = {
            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED)
                navController.popBackStack()
        }

        fun <T : Any> NavGraphBuilder.playerComposable(
            route: KClass<T>,
            content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
        ) {
            composable(route = route) { navBackStackEntry ->
                content(navBackStackEntry)

                BackHandler(enabled = sheetState.currentValue == SheetValue.Expanded) {
                    scope.launch {
                        sheetState.partialExpand()
                    }
                }
            }
        }

        playerComposable(route = Routes.NewHome::class) {
            HomeScreen(
                navController = navController
            )
        }


//        playerComposable(route = Routes.Home::class) {
//            QuickPicks(
//                openSearch = { navController.navigate(route = Routes.Search) },
//                openSettings = { navController.navigate(route = Routes.Settings) },
//                onAlbumClick = navigateToAlbum,
//                onArtistClick = navigateToArtist,
//                onPlaylistClick = { browseId ->
//                    navController.navigate(route = Routes.Playlist(id = browseId))
//                },
//                onOfflinePlaylistClick = {
//                    navController.navigate(route = Routes.BuiltInPlaylist(index = 1))
//                }
//            )
//        }

        playerComposable(route = Routes.Songs::class) {
            HomeSongs(
                openSearch = { navController.navigate(route = Routes.Search) },
                openSettings = { navController.navigate(route = Routes.Settings) },
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.Artists::class) {
            HomeArtistList(
                openSearch = { navController.navigate(route = Routes.Search) },
                openSettings = { navController.navigate(route = Routes.Settings) },
                onArtistClick = { artist -> navigateToArtist(artist.id) }
            )
        }

        playerComposable(route = Routes.Albums::class) {
            HomeAlbums(
                openSearch = { navController.navigate(route = Routes.Search) },
                openSettings = { navController.navigate(route = Routes.Settings) },
                onAlbumClick = { album -> navigateToAlbum(album.id) }
            )
        }

        playerComposable(route = Routes.Playlists::class) {
            HomePlaylists(
                openSearch = { navController.navigate(route = Routes.Search) },
                openSettings = { navController.navigate(route = Routes.Settings) },
                onBuiltInPlaylist = { playlistIndex ->
                    navController.navigate(route = Routes.BuiltInPlaylist(index = playlistIndex))
                },
                onPlaylistClick = { playlist ->
                    navController.navigate(route = Routes.LocalPlaylist(id = playlist.id))
                }
            )
        }

        playerComposable(route = Routes.Artist::class) { navBackStackEntry ->
            val route: Routes.Artist = navBackStackEntry.toRoute()

            ArtistScreen(
                browseId = route.id,
                pop = popDestination,
                onAlbumClick = navigateToAlbum,
                onArtistClick = navigateToArtist,
                onPlaylistClick = { browseId ->
                    navController.navigate(route = Routes.Playlist(id = browseId))
                }
            )
        }

        playerComposable(route = Routes.Album::class) { navBackStackEntry ->
            val route: Routes.Album = navBackStackEntry.toRoute()

            AlbumScreen(
                browseId = route.id,
                pop = popDestination,
                onAlbumClick = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.Playlist::class) { navBackStackEntry ->
            val route: Routes.Playlist = navBackStackEntry.toRoute()

            PlaylistScreen(
                browseId = route.id,
                pop = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.Settings::class) {
            SettingsScreen(
                pop = popDestination,
                onGoToSettingsPage = { index ->
                    navController.navigate(Routes.SettingsPage(index = index))
                }
            )
        }

        playerComposable(route = Routes.SettingsPage::class) { navBackStackEntry ->
            val route: Routes.SettingsPage = navBackStackEntry.toRoute()

            SettingsPage(
                section = SettingsSection.entries[route.index],
                pop = popDestination
            )
        }

        playerComposable(route = Routes.Search::class) {
            SearchScreen(
                pop = popDestination,
                onAlbumClick = navigateToAlbum,
                onArtistClick = navigateToArtist,
                onPlaylistClick = { browseId ->
                    navController.navigate(route = Routes.Playlist(id = browseId))
                }
            )
        }

        playerComposable(route = Routes.BuiltInPlaylist::class) { navBackStackEntry ->
            val route: Routes.BuiltInPlaylist = navBackStackEntry.toRoute()

            BuiltInPlaylistScreen(
                builtInPlaylist = BuiltInPlaylist.entries[route.index],
                pop = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.LocalPlaylist::class) { navBackStackEntry ->
            val route: Routes.LocalPlaylist = navBackStackEntry.toRoute()

            LocalPlaylistScreen(
                playlistId = route.id,
                pop = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }
    }
}