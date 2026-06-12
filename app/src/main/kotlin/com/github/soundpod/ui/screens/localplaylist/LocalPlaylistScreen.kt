package com.github.soundpod.ui.screens.localplaylist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.Playlist
import com.github.soundpod.query
import com.github.soundpod.ui.components.ConfirmationDialog
import com.github.soundpod.ui.components.PlaylistScreenLayout
import com.github.soundpod.ui.components.TextFieldDialog
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun LocalPlaylistScreen(
    playlistId: Long,
    pop: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    var playlist: Playlist? by remember { mutableStateOf(null) }

    var isRenaming by rememberSaveable { mutableStateOf(false) }
    var isDeleting by rememberSaveable { mutableStateOf(false) }
    val (colorPalette) = LocalAppearance.current

    var isEditMode by remember { mutableStateOf(false) }
    var selectedUids by remember { mutableStateOf(emptySet<String>()) }
    var isSearching by remember { mutableStateOf(false) }

    var songCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        db.playlist(playlistId).filterNotNull().collect { playlist = it }
    }

    LaunchedEffect(Unit) {
        db.playlistSongs(playlistId).collect { songs ->
            songCount = songs.size
        }
    }

    BackHandler(enabled = isEditMode || isSearching) {
        if (isSearching) {
            isSearching = false
        } else if (isEditMode) {
            isEditMode = false
            selectedUids = emptySet()
        }
    }

    PlaylistScreenLayout(
        title = {
            Column {
                Text(
                    text = playlist?.name ?: "",
                    style = typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorPalette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pluralStringResource(id = R.plurals.number_of_songs, count = songCount, songCount),
                    style = typography.bodySmall,
                    color = colorPalette.text.copy(alpha = 0.7f)
                )
            }
        },
        onBackClick = {
            if (isSearching) {
                isSearching = false
            } else if (isEditMode) {
                isEditMode = false
                selectedUids = emptySet()
            } else {
                pop()
            }
        },
        actions = {
            IconButton(
                onClick = onSearchClick
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colorPalette.text
                )
            }
        },
        dropDownMenuContent = { dismissMenu ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.rename_playlist),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    isRenaming = true
                    dismissMenu()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.delete_playlist),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    isDeleting = true
                    dismissMenu()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(id = R.string.settings),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    onSettingsClick()
                    dismissMenu()
                }
            )
        },
        headerContent = {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorPalette.text.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colorPalette.text.copy(alpha = 0.4f)
                )
            }
        },
        content = {
            LocalPlaylistSongs(
                playlistId = playlistId,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist
            )
        }
    )

    if (isRenaming) {
        TextFieldDialog(
            title = stringResource(id = R.string.rename_playlist),
            hintText = stringResource(id = R.string.playlist_name_hint),
            initialTextInput = playlist?.name ?: "",
            onDismiss = { isRenaming = false },
            onDone = { text ->
                query {
                    playlist?.copy(name = text)
                        ?.let(db::update)
                }
            }
        )
    }

    if (isDeleting) {
        ConfirmationDialog(
            title = stringResource(id = R.string.delete_playlist_dialog),
            onDismiss = { isDeleting = false },
            onConfirm = {
                query {
                    playlist?.let(db::delete)
                }
                pop()
            }
        )
    }
}