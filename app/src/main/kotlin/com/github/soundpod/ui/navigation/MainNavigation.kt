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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.ui.screens.album.AlbumScreen
import com.github.soundpod.ui.screens.artist.ArtistScreen
import com.github.soundpod.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import com.github.soundpod.ui.screens.favorites.FavoriteTracksScreen
import com.github.soundpod.ui.screens.favorites.FavoritesScreen
import com.github.soundpod.ui.screens.home.HomeScreen
import com.github.soundpod.ui.screens.localplaylist.LocalPlaylistScreen
import com.github.soundpod.ui.screens.playlist.PlaylistScreen
import com.github.soundpod.ui.screens.search.SearchResult
import com.github.soundpod.ui.screens.search.SearchScreen
import kotlinx.coroutines.launch
import kotlin.reflect.KClass


@UnstableApi
@Composable
fun MainNavigation(
    navController: NavHostController,
    sheetState: SheetState,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Routes.Home,
        enterTransition = { Transitions.enter() },
        exitTransition = { Transitions.exit() },
        popEnterTransition = { Transitions.popEnter() },
        popExitTransition = { Transitions.popExit() }
    ) {
        val navigateToAlbum = { browseId: String ->
            navController.navigate(route = Routes.Album(id = browseId)) {
                launchSingleTop = true
                restoreState = true
            }
        }

        val navigateToArtist = { browseId: String ->
            navController.navigate(route = Routes.Artist(id = browseId)) {
                launchSingleTop = true
                restoreState = true
            }
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

        playerComposable(route = Routes.Home::class) {
            HomeScreen(
                navController = navController,
                onSettingsClick = onNavigateToSettings
            )
        }

        composable<Routes.SearchResult> { navBackStackEntry ->
            val route: Routes.SearchResult = navBackStackEntry.toRoute()

            SearchResult(
                navController = navController,
                query = route.query,
                resultType = route.type
            )
        }

        playerComposable(route = Routes.Artist::class) { navBackStackEntry ->
            val route: Routes.Artist = navBackStackEntry.toRoute()

            ArtistScreen(
                browseId = route.id,
                onBack = { navController.popBackStack() },
                onSearchClick = { navController.navigate(route = Routes.Search) },
                onSettingsClick = onNavigateToSettings,
                onAlbumClick = navigateToAlbum,
                onArtistClick = navigateToArtist
            )
        }

        playerComposable(route = Routes.Album::class) { navBackStackEntry ->
            val route: Routes.Album = navBackStackEntry.toRoute()

            AlbumScreen(
                browseId = route.id,
                onGoToArtist = navigateToArtist,
                onSearchClick = {navController.navigate(route = Routes.Search)},
                onSettingsClick = onNavigateToSettings,
                onBack = {navController.popBackStack()}
            )
        }

        playerComposable(route = Routes.Playlist::class) { navBackStackEntry ->
            val route: Routes.Playlist = navBackStackEntry.toRoute()

            PlaylistScreen(
                browseId = route.id,
                onBack = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.Search::class) {
            SearchScreen(
                initialTextInput = "",
                navController = navController,
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
                onGoToArtist = navigateToArtist,
                onSearchClick = { navController.navigate(route = Routes.Search)},
                onSettingsClick = onNavigateToSettings
            )
        }

        composable(route = Routes.LocalPlaylist::class) { navBackStackEntry ->
            val route: Routes.LocalPlaylist = navBackStackEntry.toRoute()

            LocalPlaylistScreen(
                playlistId = route.id,
                pop = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist,
                onSearchClick = { navController.navigate(route = Routes.Search) },
                onSettingsClick = onNavigateToSettings
            )
        }

        playerComposable(route = Routes.Favorites::class) {
            FavoritesScreen(
                onBackClick = { navController.popBackStack() },
                onFavoriteTracksClick = { navController.navigate(route = Routes.FavoriteTracks) },
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.FavoriteTracks::class) {
            FavoriteTracksScreen(
                onBackClick = { navController.popBackStack() },
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }
    }
}
