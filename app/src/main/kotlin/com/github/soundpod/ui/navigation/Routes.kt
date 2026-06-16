package com.github.soundpod.ui.navigation

import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable
    data object Home

    @Serializable
    data class Artist(val id: String)

    @Serializable
    data class ArtistSongs(val id: String, val params: String? = null)

    @Serializable
    data class Album(val id: String)

    @Serializable
    data class Playlist(val id: String)

    @Serializable
    data object Player

    @Serializable
    data object Search

    @Serializable
    data object SearchResult
    @Serializable
    data class BuiltInPlaylist(val index: Int)

    @Serializable
    data class LocalPlaylist(val id: Long)

    @Serializable
    data object Favorites
}


//settings screen moved to activity.launch
object SettingsDestinations {
    const val MAIN = "settings_main"
    const val APPEARANCE = "settings_appearance"
    const val BACKGROUND = "background"
    const val PLAYER = "settings_player"
    const val PRIVACY = "settings_privacy"
    const val BACKUP = "settings_backup"
    const val DATABASE = "settings_database"
    const val MORE = "settings_more"
    const val EXPERIMENT = "settings_experiment"
    const val ABOUT = "settings_about"
    const val SLEEP_TIMER = "settings_sleep_timer"
    const val QUICK_PICKS = "settings_quick_picks"
    const val TRACK_DETAILS = "settings_track_details"
    const val ACCOUNT = "settings_account"
    const val LOGIN = "settings_login"
}
