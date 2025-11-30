//package com.github.soundpod.ui.navigation
//
//import com.github.soundpod.ui.screens.settings.SettingsScreen
//
//composable(
//route = Routes.Settings::class,
//enterTransition = { Transitions.enter() },
//exitTransition = { Transitions.exit() },
//popEnterTransition = { Transitions.popEnter() },
//popExitTransition = { Transitions.popExit() }
//) {
//    SettingsScreen(
//        navController = navController,
//        onBackClick = { navController.popBackStack() }
//    )
//}
//
//composable(route = Routes.Appearance::class) {
//    Appearance(
//    onBackClick = { navController.popBackStack() }
//    )
//}
//
//composable(route = Routes.Player::class) {
//    PlayerSettings(
//    onBackClick = { navController.popBackStack() }
//    )
//}
//
//composable(route = Routes.Privacy::class) {
//    Privacy(
//    onBackClick = { navController.popBackStack() }
//    )
//}
//
//composable(route = Routes.Backup::class) {
//    Backup(
//    onBackClick = { navController.popBackStack() }
//    )
//}
//
//composable(route = Routes.Storage::class) {
//    CacheSettings(
//    onBackClick = { navController.popBackStack() }
//    )
//}
//
//composable(route = Routes.More::class) {
//    MoreSettings(
//    onBackClick = { navController.popBackStack() }
//    )
//}
//
//composable(route = Routes.Experiment::class) {
//    Experiment(
//    onBackClick = { navController.popBackStack() }
//    )
//}
//
//composable(route = Routes.About::class) {
//    AboutSettings (
//    onBackClick = { navController.popBackStack() }
//    )
//}
