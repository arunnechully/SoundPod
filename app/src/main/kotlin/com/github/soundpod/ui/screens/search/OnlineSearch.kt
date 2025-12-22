package com.github.soundpod.ui.screens.search

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.innertube.Innertube
import com.github.innertube.requests.searchPage
import com.github.innertube.utils.from
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.LocalMenuState
import com.github.soundpod.ui.components.LoadingAnimation
import com.github.soundpod.ui.components.NonQueuedMediaItemMenu
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.items.SongItem
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.forcePlay

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun OnlineSearch(
    query: String,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onViewAllClick: (String) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    var searchResults by remember { mutableStateOf<List<Innertube.SongItem>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchResults = null
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true

        try {
            val result = Innertube.searchPage(
                query = query,
                params = Innertube.SearchFilter.Song.value,
                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
            )
            searchResults = result?.getOrNull()?.items?.take(5)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }
    if (isLoading) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
        ) {
            LoadingAnimation(
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else if (searchResults?.isNotEmpty() == true) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.songs),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = colorPalette.text.copy(alpha = 0.5f)
                )
            }

            SettingsCard(
                content = {
                    Column {
                        searchResults!!.forEach { song ->
                            SongItem(
                                song = song,
                                onClick = {
                                    binder?.stopRadio()
                                    binder?.player?.forcePlay(song.asMediaItem)
                                    binder?.setupRadio(song.info?.endpoint)
                                },
                                onLongClick = {
                                    menuState.display {
                                        NonQueuedMediaItemMenu(
                                            onDismiss = menuState::hide,
                                            mediaItem = song.asMediaItem,
                                            onGoToAlbum = onAlbumClick,
                                            onGoToArtist = onArtistClick
                                        )
                                    }
                                }
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .fillMaxWidth(),
                            color = colorPalette.text.copy(alpha = 0.1f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { onViewAllClick("Songs") },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = colorPalette.text
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "View all",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}