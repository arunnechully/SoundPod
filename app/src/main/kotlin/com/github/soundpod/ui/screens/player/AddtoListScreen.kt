package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.enums.PlaylistSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.Playlist
import com.github.soundpod.models.SongPlaylistMap
import com.github.soundpod.transaction
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.TextFieldDialog
import com.github.soundpod.ui.screens.settings.SettingsColumn
import com.github.soundpod.ui.styling.px
import com.github.soundpod.utils.thumbnail
import com.github.soundpod.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun AddToListScreen() {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val mediaItem = binder?.player?.currentMediaItem

    val addedToPlaylistMessage = stringResource(R.string.added_to_playlist)
    val noPlaylistsYetMessage = stringResource(R.string.playlist_place_holder)
    val noSongPlayingMessage = stringResource(R.string.no_song_playing)

    val playlists by remember {
        db.playlistPreviews(PlaylistSortBy.DateAdded, SortOrder.Descending)
    }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

    var isCreatingNewPlaylist by rememberSaveable { mutableStateOf(false) }

    val playlistName = stringResource(R.string.playlist)

    if (isCreatingNewPlaylist) {
        TextFieldDialog(
            title = stringResource(id = R.string.new_playlist),
            hintText = stringResource(id = R.string.playlist_name_hint),
            initialTextInput = "$playlistName %03d".format(playlists.size + 1),
            onDismiss = { isCreatingNewPlaylist = false },
            onDone = { text ->
                if (mediaItem != null) {
                    transaction {
                        val playlistId = db.insert(Playlist(name = text))
                        if (playlistId != -1L) {
                            db.insert(mediaItem)
                            db.insert(
                                SongPlaylistMap(
                                    songId = mediaItem.mediaId,
                                    playlistId = playlistId,
                                    position = 0
                                )
                            )
                        }
                    }
                    context.toast(addedToPlaylistMessage)
                } else {
                    context.toast(noSongPlayingMessage)
                }
                isCreatingNewPlaylist = false
            }
        )
    }

    SettingsCard(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn {
            item {
                PlaylistRowItem(
                    title = stringResource(id = R.string.new_playlist),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colorPalette.accent,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    onClick = { isCreatingNewPlaylist = true }
                )
            }

            items(playlists, key = { it.id }) { playlistPreview ->
                val thumbnailWidthPx = 100.dp.px
                val thumbnails by remember(playlistPreview.id, thumbnailWidthPx) {
                    db.playlistThumbnailUrls(playlistPreview.id).distinctUntilChanged().map {
                        it.map { url -> url.thumbnail(size = thumbnailWidthPx / 2) }
                    }
                }.collectAsState(initial = emptyList(), context = Dispatchers.IO)

                PlaylistRowItem(
                    title = playlistPreview.name,
                    thumbnailContent = {
                        if (thumbnails.toSet().size == 1) {
                            AsyncImage(
                                model = thumbnails.firstOrNull()?.thumbnail(thumbnailWidthPx),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                listOf(
                                    Alignment.TopStart,
                                    Alignment.TopEnd,
                                    Alignment.BottomStart,
                                    Alignment.BottomEnd
                                ).forEachIndexed { index, alignment ->
                                    AsyncImage(
                                        model = thumbnails.getOrNull(index),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .align(alignment)
                                            .fillMaxSize(0.5f)
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        if (mediaItem != null) {
                            transaction {
                                db.insert(mediaItem)
                                db.insert(
                                    SongPlaylistMap(
                                        songId = mediaItem.mediaId,
                                        playlistId = playlistPreview.id,
                                        position = 0
                                    )
                                )
                            }
                            context.toast(addedToPlaylistMessage)
                        } else {
                            context.toast(noSongPlayingMessage)
                        }
                    }
                )
            }
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = noPlaylistsYetMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorPalette.text.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaylistRowItem(
    title: String,
    onClick: () -> Unit,
    thumbnailContent: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
) {
    val (colorPalette) = LocalAppearance.current

    SettingsColumn(
        title = title,
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(percent = 25))
                    .background(colorPalette.accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                thumbnailContent?.invoke()
                icon?.invoke()
            }
        },
        showDivider = true,
        verticalPadding = 6.dp,
        onClick = onClick
    )
}
