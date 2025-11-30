package com.github.soundpod.ui.navigation

import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable
    data object Home

    @Serializable
    data class Artist(val id: String)

    @Serializable
    data class Album(val id: String)

    @Serializable
    data class Playlist(val id: String)

    @Serializable
    data object Player

    @Serializable
    data object Search

    @Serializable
    data class BuiltInPlaylist(val index: Int)

    @Serializable
    data class LocalPlaylist(val id: Long)
}


//settings screen moved to activity.launch
object SettingsDestinations {
    const val MAIN = "settings_main"
    const val APPEARANCE = "settings_appearance"
    const val PLAYER = "settings_player"
    const val PRIVACY = "settings_privacy"
    const val BACKUP = "settings_backup"
    const val DATABASE = "settings_database"
    const val MORE = "settings_more"
    const val EXPERIMENT = "settings_experiment"
    const val ABOUT = "settings_about"
}