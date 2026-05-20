package com.github.soundpod.ui.screens.builtinplaylist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.github.core.ui.favoritesIcon
import com.github.soundpod.R
import com.github.soundpod.enums.BuiltInPlaylist
import com.github.soundpod.enums.SongSortBy
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.songSortByKey
import com.github.soundpod.utils.songSortOrderKey

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun NewBuiltInPlaylistScreen(
    builtInPlaylist: BuiltInPlaylist,
    pop: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    var isEditMode by remember { mutableStateOf(false) }
    var selectedUids by remember { mutableStateOf(emptySet<String>()) }
    var isSearching by remember { mutableStateOf(false) }

    var songCount by remember { mutableIntStateOf(0) }

    var sortBy by rememberPreference(songSortByKey, SongSortBy.Title)
    var sortOrder by rememberPreference(songSortOrderKey, SortOrder.Ascending)

    BackHandler(enabled = isEditMode || isSearching) {
        if (isSearching) {
            isSearching = false
        } else if (isEditMode) {
            isEditMode = false
            selectedUids = emptySet()
        }
    }

    SettingsScreenLayout(
        title = {},
        scrollable = false,
        horizontalPadding = 0.dp,
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
            AnimatedVisibility(
                visible = isEditMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                IconButton(onClick = { /* TODO */ }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Selected"
                    )
                }
            }

            IconButton(onClick = { isSearching = !isSearching }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Centered Playlist Cover Placeholder
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorPalette.text.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = when (builtInPlaylist) {
                        BuiltInPlaylist.Favorites -> painterResource(id = R.drawable.heart)
                        BuiltInPlaylist.Offline -> painterResource(id = R.drawable.offline)
                    },
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = when (builtInPlaylist) {
                        BuiltInPlaylist.Favorites -> colorPalette.favoritesIcon.copy(alpha = 0.5f)
                        BuiltInPlaylist.Offline -> colorPalette.text.copy(alpha = 0.4f)
                    },
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Centered Title Text
            Text(
                text = when (builtInPlaylist) {
                    BuiltInPlaylist.Favorites -> stringResource(id = R.string.favorites)
                    BuiltInPlaylist.Offline -> stringResource(id = R.string.offline)
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorPalette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Track Count Text
            Text(
                text = pluralStringResource(id = R.plurals.number_of_songs, count = songCount, songCount),
                style = MaterialTheme.typography.bodyMedium,
                color = colorPalette.text.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsCard(
                shape = RoundedCornerShape(
                    topStart = 25.dp,
                    topEnd = 25.dp
                )
            ) {
                NewBuiltInPlaylistSongs(
                    builtInPlaylist = builtInPlaylist,
                    isEditMode = isEditMode,
                    onEditModeChange = { isEditMode = it },
                    selectedUids = selectedUids,
                    onSelectedUidsChange = { selectedUids = it },
                    onGoToAlbum = onGoToAlbum,
                    onGoToArtist = onGoToArtist,
                    sortBy = sortBy,
                    onSortByChange = { sortBy = it },
                    sortOrder = sortOrder,
                    onSortOrderChange = { sortOrder = it },
                    onSongsCountChange = { songCount = it }
                )
            }
        }
    }
}