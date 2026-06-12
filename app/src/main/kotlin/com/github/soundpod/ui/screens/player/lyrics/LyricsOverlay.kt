package com.github.soundpod.ui.screens.player.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaMetadata
import com.github.core.ui.LocalAppearance
import com.github.soundpod.ui.appearance.LoadingAnimation
import com.github.soundpod.ui.components.Overlay
import com.github.soundpod.utils.LyricsData
import com.github.soundpod.viewmodels.LyricsViewModel

@Composable
fun LyricsOverlay(
    modifier: Modifier = Modifier,
    mediaId: String?,
    mediaMetadata: MediaMetadata?,
    viewModel: LyricsViewModel = viewModel()
) {
    val lazyListState = rememberLazyListState()

    val (colorPalette) = LocalAppearance.current
    val lyricsData by viewModel.lyricsData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(mediaId) {
        viewModel.loadLyrics(mediaId)
    }

    Overlay(
        modifier = modifier,
        lazyListState = lazyListState,
        enableScrollbar = false,
        headerContent = {
            Text(
                text = mediaMetadata?.title?.toString() ?: "Lyrics",
                style = MaterialTheme.typography.titleMedium,
                color = colorPalette.text,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp),
                maxLines = 1
            )
        }
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingAnimation(modifier = Modifier.size(50.dp))
            }
        } else {
            when (val currentLyrics = lyricsData) {
                is LyricsData.Unsynced -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        item {
                            Text(
                                text = currentLyrics.text,
                                color = colorPalette.text,
                                fontSize = 18.sp,
                                lineHeight = 32.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                LyricsData.None -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No lyrics found",
                            color = colorPalette.textDisabled,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
