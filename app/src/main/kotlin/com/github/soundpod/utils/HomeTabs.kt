package com.github.soundpod.utils

import androidx.annotation.StringRes
import com.github.soundpod.R

enum class HomeTab(
    @StringRes val title: Int,
    val key: String
) {
    Home(R.string.home, showHomeTabKey),
    Favorites(R.string.favorites, showFavoritesTabKey),
    Following(R.string.following, showFollowingTabKey),
    Songs(R.string.songs, showSongsTabKey),
    Artists(R.string.artists, showArtistsTabKey),
    Albums(R.string.albums, showAlbumsTabKey),
    Playlists(R.string.playlists, showPlaylistsTabKey)
}
