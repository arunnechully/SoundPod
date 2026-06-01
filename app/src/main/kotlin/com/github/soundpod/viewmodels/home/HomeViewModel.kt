package com.github.soundpod.viewmodels.home

import androidx.lifecycle.ViewModel
import com.github.soundpod.R

class HomeViewModel : ViewModel() {
    val tabs = listOf(
        R.string.home,
        R.string.songs,
        R.string.artists,
        R.string.albums,
        R.string.playlists
    )
}
