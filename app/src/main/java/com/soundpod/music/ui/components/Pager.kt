package com.soundpod.music.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.soundpod.music.data.Song
import com.soundpod.music.ui.screens.Tracks

//@Composable
//fun TabsPager(tabs: List<String>, pagerState: PagerState) {
//
//    HorizontalPager(
//        state = pagerState,
//        modifier = Modifier.fillMaxSize()
//    ) { page ->
//
//        when (page) {
//            0 -> { Tracks() }
//            1 -> {}
//
//            2 -> {}
//
//            else -> {}
//        }
//    }
//}
