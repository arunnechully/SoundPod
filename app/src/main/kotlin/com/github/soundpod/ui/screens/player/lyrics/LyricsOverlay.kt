package com.github.soundpod.ui.screens.player.lyrics

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaMetadata
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Lyrics
import com.github.soundpod.query
import com.github.soundpod.ui.appearance.LoadingAnimation
import com.github.soundpod.ui.components.Menu
import com.github.soundpod.ui.components.MenuEntry
import com.github.soundpod.ui.components.Overlay
import com.github.soundpod.ui.components.TextFieldDialog
import com.github.soundpod.ui.components.TextPlaceholder
import com.github.soundpod.ui.styling.onOverlay
import com.github.soundpod.utils.LyricsData
import com.github.soundpod.utils.toast
import com.github.soundpod.utils.verticalFadingEdge
import com.github.soundpod.viewmodels.LyricsViewModel
import com.valentinilk.shimmer.shimmer

@Composable
fun LyricsOverlay(
    modifier: Modifier = Modifier,
    mediaId: String?,
    mediaMetadata: MediaMetadata?,
    currentPositionMs: Long = 0,
    onSeekTo: (Long) -> Unit = {},
    viewModel: LyricsViewModel = viewModel()
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val lazyListState = rememberLazyListState()

    val (colorPalette) = LocalAppearance.current
    val lyricsData by viewModel.lyricsData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var isEditing by remember(mediaId) { mutableStateOf(false) }

    LaunchedEffect(mediaId) {
        viewModel.loadLyrics(mediaId)
    }

    val syncedLines = (lyricsData as? LyricsData.Synced)?.lines ?: emptyList()
    val currentLineIndex by remember(currentPositionMs, syncedLines) {
        derivedStateOf {
            syncedLines.indexOfLast { it.startTime <= currentPositionMs }.coerceAtLeast(0)
        }
    }

    LaunchedEffect(currentLineIndex) {
        if (syncedLines.isNotEmpty()) {
            lazyListState.animateScrollToItem(currentLineIndex, -200)
        }
    }

    if (isEditing && mediaId != null) {
        val currentText = when (val data = lyricsData) {
            is LyricsData.Unsynced -> data.text
            else -> ""
        }
        TextFieldDialog(
            title = stringResource(id = R.string.edit_lyrics),
            hintText = stringResource(id = R.string.enter_lyrics),
            initialTextInput = currentText,
            singleLine = false,
            maxLines = 10,
            isTextInputValid = { true },
            onDismiss = { isEditing = false },
            onDone = {
                query {
                    db.upsert(
                        Lyrics(
                            songId = mediaId,
                            fixed = it,
                            synced = null
                        )
                    )
                }
                isEditing = false
            }
        )
    }

    Box(modifier = modifier) {
        Overlay(
            modifier = Modifier.fillMaxSize(),
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
                    is LyricsData.Synced -> {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 150.dp, horizontal = 24.dp)
                        ) {
                            itemsIndexed(currentLyrics.lines) { index, line ->
                                val isCurrent = index == currentLineIndex
                                val color by animateColorAsState(
                                    targetValue = if (isCurrent) colorPalette.text else colorPalette.textDisabled,
                                    label = "LyricColor"
                                )
                                val scale by animateFloatAsState(
                                    targetValue = if (isCurrent) 1.05f else 1f,
                                    label = "LyricScale"
                                )

                                Text(
                                    text = line.text,
                                    color = color,
                                    fontSize = 22.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    lineHeight = 36.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .alpha(if (isCurrent) 1f else 0.6f)
                                        .clickable { onSeekTo(line.startTime) }
                                )
                            }
                        }
                    }

                    is LyricsData.Unsynced -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalFadingEdge()
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
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

        IconButton(
            onClick = {
                menuState.display {
                    Menu {
                        MenuEntry(
                            icon = Icons.Outlined.Edit,
                            text = stringResource(id = R.string.edit_lyrics),
                            enabled = mediaId != null,
                            onClick = {
                                menuState.hide()
                                isEditing = true
                            }
                        )

                        MenuEntry(
                            icon = Icons.Outlined.Search,
                            text = stringResource(id = R.string.search_lyrics_online),
                            onClick = {
                                menuState.hide()
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_WEB_SEARCH).apply {
                                            putExtra(
                                                SearchManager.QUERY,
                                                "${mediaMetadata?.title ?: ""} ${mediaMetadata?.artist ?: ""} lyrics"
                                            )
                                        }
                                    )
                                } catch (_: ActivityNotFoundException) {
                                    context.toast("Couldn't find an application to browse the Internet")
                                }
                            }
                        )

                        MenuEntry(
                            icon = Icons.Outlined.Download,
                            text = stringResource(id = R.string.fetch_lyrics_again),
                            enabled = mediaId != null,
                            onClick = {
                                menuState.hide()
                                query {
                                    db.upsert(
                                        Lyrics(
                                            songId = mediaId!!,
                                            fixed = null,
                                            synced = null,
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = "Lyrics Menu",
                tint = MaterialTheme.colorScheme.onOverlay
            )
        }
    }
}
