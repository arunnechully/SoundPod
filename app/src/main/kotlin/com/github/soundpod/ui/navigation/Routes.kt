package com.github.soundpod.ui.navigation

import kotlinx.serialization.Serializable

sealed class Routes {
    @Serializable
    data object NewHome

    @Serializable
    data object Home

    @Serializable
    data object Songs

    @Serializable
    data object Artists

    @Serializable
    data object Albums

    @Serializable
    data object Playlists

    @Serializable
    data class Artist(val id: String)

    @Serializable
    data class Album(val id: String)

    @Serializable
    data class Playlist(val id: String)

    @Serializable
    data object Settings

    @Serializable
    data class SettingsPage(val index: Int)

    @Serializable
    data object Appearance

    @Serializable
    data object Player

    @Serializable
    data object Privacy

    @Serializable
    data object Backup

    @Serializable
    data object Storage

    @Serializable
    data object More

    @Serializable
    data object Experiment

    @Serializable
    data object About
    @Serializable
    data object Search

    @Serializable
    data class BuiltInPlaylist(val index: Int)

    @Serializable
    data class LocalPlaylist(val id: Long)
}