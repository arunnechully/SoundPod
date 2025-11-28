@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)

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
import com.github.soundpod.ui.screens.album.AlbumScreen
import com.github.soundpod.ui.screens.artist.ArtistScreen
import com.github.soundpod.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import com.github.soundpod.ui.screens.home.HomeScreen
import com.github.soundpod.ui.screens.localplaylist.LocalPlaylistScreen
import com.github.soundpod.ui.screens.playlist.PlaylistScreen
import com.github.soundpod.ui.screens.search.SearchScreen
import com.github.soundpod.ui.screens.settings.Appearance
import com.github.soundpod.ui.screens.settings.Backup
import com.github.soundpod.ui.screens.settings.CacheSettings
import com.github.soundpod.ui.screens.settings.Experiment
import com.github.soundpod.ui.screens.settings.MoreSettings
import com.github.soundpod.ui.screens.settings.AboutSettings
import com.github.soundpod.ui.screens.settings.PlayerSettings
import com.github.soundpod.ui.screens.settings.SettingsScreen
import com.github.soundpod.ui.screens.settings.Privacy
import com.github.soundpod.utils.homeScreenTabIndexKey
import com.github.soundpod.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.reflect.KClass


@Composable
fun Navigation(
    navController: NavHostController,
    sheetState: SheetState
) {
    val scope = rememberCoroutineScope()
    val (screenIndex, _) = rememberPreference(homeScreenTabIndexKey, defaultValue = 0)

    NavHost(
        navController = navController,
        startDestination = TopDestinations.routes.getOrElse(
            index = screenIndex,
            defaultValue = { Routes.NewHome }
        )::class,
        enterTransition = { Transitions.enter() },
        exitTransition = { Transitions.exit() },
        popEnterTransition = { Transitions.popEnter() },
        popExitTransition = { Transitions.popExit() }
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

        composable(
            route = Routes.Settings::class,
            enterTransition = { Transitions.enter() },
            exitTransition = { Transitions.exit() },
            popEnterTransition = { Transitions.popEnter() },
            popExitTransition = { Transitions.popExit() }
        ) {
            SettingsScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.Appearance::class) {
            Appearance(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.Player::class) {
            PlayerSettings(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.Privacy::class) {
            Privacy(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.Backup::class) {
            Backup(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.Storage::class) {
            CacheSettings(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.More::class) {
            MoreSettings(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.Experiment::class) {
            Experiment(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.About::class) {
            AboutSettings (
                onBackClick = { navController.popBackStack() }
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

        composable(route = Routes.BuiltInPlaylist::class) { navBackStackEntry ->
            val route: Routes.BuiltInPlaylist = navBackStackEntry.toRoute()

            BuiltInPlaylistScreen(
                builtInPlaylist = BuiltInPlaylist.entries[route.index],
                pop = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        composable(route = Routes.LocalPlaylist::class) { navBackStackEntry ->
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