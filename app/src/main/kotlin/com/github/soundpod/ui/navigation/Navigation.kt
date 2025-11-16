@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)

package com.github.soundpod.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.github.soundpod.ui.screens.home.HomeScreen
import com.github.soundpod.ui.screens.localplaylist.LocalPlaylistScreen
import com.github.soundpod.ui.screens.playlist.PlaylistScreen
import com.github.soundpod.ui.screens.search.SearchScreen
import com.github.soundpod.ui.screens.settings.SettingsPage
import com.github.soundpod.ui.screens.settings.newsettings.Appearance
import com.github.soundpod.ui.screens.settings.newsettings.Experiment
import com.github.soundpod.ui.screens.settings.newsettings.MoreSettings
import com.github.soundpod.ui.screens.settings.newsettings.NewAboutSettings
import com.github.soundpod.ui.screens.settings.newsettings.NewPlayerSettings
import com.github.soundpod.ui.screens.settings.newsettings.NewSettingsScreen
import com.github.soundpod.ui.screens.settings.newsettings.Privacy
import com.github.soundpod.ui.screens.settings.newsettings.CacheSettings
import com.github.soundpod.utils.homeScreenTabIndexKey
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.ui.screens.settings.newsettings.Backup
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

private fun defaultEnterTransition() =
    slideInHorizontally(
        initialOffsetX = { it / 4 }, // 25% offset instead of full width
        animationSpec = tween(
            durationMillis = 220,
            easing = LinearOutSlowInEasing
        )
    ) + fadeIn(animationSpec = tween(200))

private fun defaultExitTransition() =
    slideOutHorizontally(
        targetOffsetX = { -it / 4 },
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutLinearInEasing
        )
    ) + fadeOut(animationSpec = tween(150))

private fun defaultPopEnterTransition() =
    slideInHorizontally(
        initialOffsetX = { -it / 4 },
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearOutSlowInEasing
        )
    ) + fadeIn(animationSpec = tween(200))

private fun defaultPopExitTransition() =
    slideOutHorizontally(
        targetOffsetX = { it / 4 },
        animationSpec = tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(animationSpec = tween(150))


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
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
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

        composable(route = Routes.Artist::class) { navBackStackEntry ->
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

        composable(route = Routes.Album::class) { navBackStackEntry ->
            val route: Routes.Album = navBackStackEntry.toRoute()

            AlbumScreen(
                browseId = route.id,
                pop = popDestination,
                onAlbumClick = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        composable(route = Routes.Playlist::class) { navBackStackEntry ->
            val route: Routes.Playlist = navBackStackEntry.toRoute()

            PlaylistScreen(
                browseId = route.id,
                pop = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        composable(route = Routes.Settings::class) {
            NewSettingsScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }
//
//        composable(route = Routes.SettingsPage::class) { navBackStackEntry ->
//            val route: Routes.SettingsPage = navBackStackEntry.toRoute()
//
//            SettingsPage(
//                section = SettingsSection.entries[route.index],
//                pop = popDestination
//            )
//        }

        composable(route = Routes.Appearance::class) {
            Appearance(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.Player::class) {
            NewPlayerSettings(
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
            NewAboutSettings (
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Routes.Search::class) {
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