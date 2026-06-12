package com.github.soundpod.ui.screens.player.lyrics

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import com.github.innertube.Innertube
import com.github.innertube.requests.lyrics
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.models.Lyrics
import com.github.soundpod.query
import com.github.soundpod.ui.components.Menu
import com.github.soundpod.ui.components.MenuEntry
import com.github.soundpod.ui.components.TextFieldDialog
import com.github.soundpod.ui.components.TextPlaceholder
import com.github.soundpod.ui.styling.onOverlay
import com.github.soundpod.utils.isLandscape
import com.github.soundpod.utils.toast
import com.github.soundpod.utils.verticalFadingEdge
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OldLyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    size: Dp,
    mediaMetadataProvider: () -> MediaMetadata,
    ensureSongInserted: () -> Unit,
    fullScreenLyrics: Boolean,
    toggleFullScreenLyrics: () -> Unit
) {
    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val context = LocalContext.current
        val menuState = LocalMenuState.current

        var isEditing by remember(mediaId) {
            mutableStateOf(false)
        }

        var lyrics by remember {
            mutableStateOf<Lyrics?>(null)
        }

        val text = lyrics?.fixed

        var isError by remember(mediaId) {
            mutableStateOf(false)
        }

        LaunchedEffect(mediaId) {
            withContext(Dispatchers.IO) {
                db.lyrics(mediaId).collect {
                    if (it?.fixed == null) {
                        Innertube.lyrics(videoId = mediaId)?.onSuccess { fixedLyrics ->
                            db.upsert(
                                Lyrics(
                                    songId = mediaId,
                                    fixed = fixedLyrics ?: "",
                                    synced = null
                                )
                            )
                        }?.onFailure {
                            isError = true
                        }
                    } else {
                        lyrics = it
                    }
                }
            }
        }

        if (isEditing) {
            TextFieldDialog(
                title = stringResource(id = R.string.edit_lyrics),
                hintText = stringResource(id = R.string.enter_lyrics),
                initialTextInput = text ?: "",
                singleLine = false,
                maxLines = 10,
                isTextInputValid = { true },
                onDismiss = { isEditing = false },
                onDone = {
                    query {
                        ensureSongInserted()
                        db.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = it,
                                synced = null,
                            )
                        )
                    }
                }
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .fillMaxSize()
                .background(Color.Black.copy(0.45f))
        ) {
            AnimatedVisibility(
                visible = isError && text == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = stringResource(id = R.string.error_fetching_lyrics),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = text?.let(String::isEmpty) == true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = stringResource(id = R.string.lyrics_not_available),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(all = 16.dp)
                        .fillMaxWidth()
                )
            }

            if (text?.isNotEmpty() == true) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .verticalFadingEdge()
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                        .padding(vertical = size / 4, horizontal = 32.dp)
                )
            }

            if (text == null && !isError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.shimmer()
                ) {
                    repeat(4) {
                        TextPlaceholder(
                            modifier = Modifier.alpha(1f - it * 0.2f)
                        )
                    }
                }
            }

            if (!isLandscape) {
                IconButton(
                    onClick = toggleFullScreenLyrics,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Icon(
                        imageVector = if (fullScreenLyrics) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onOverlay
                    )
                }
            }

            IconButton(
                onClick = {
                    menuState.display {
                        Menu {
                            MenuEntry(
                                icon = Icons.Outlined.Edit,
                                text = stringResource(id = R.string.edit_lyrics),
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
                                    val mediaMetadata = mediaMetadataProvider()

                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                putExtra(
                                                    SearchManager.QUERY,
                                                    "${mediaMetadata.title ?: ""} ${mediaMetadata.artist ?: ""} lyrics"
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
                                enabled = lyrics != null,
                                onClick = {
                                    menuState.hide()
                                    query {
                                        db.upsert(
                                            Lyrics(
                                                songId = mediaId,
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
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onOverlay
                )
            }
        }
    }
}
