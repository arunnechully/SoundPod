package com.github.soundpod.ui.screens.album

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.ui.components.PlaylistScreenLayout
import com.github.soundpod.viewmodels.AlbumViewModel

@UnstableApi
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumScreen(
    browseId: String,
    onGoToArtist: (String) -> Unit,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: AlbumViewModel = viewModel(),
) {
    BackHandler { onBack() }

    LaunchedEffect(browseId) {
        viewModel.initAlbum(browseId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val album = uiState.album
    val (colorPalette) = LocalAppearance.current

    PlaylistScreenLayout(
        title = {
            Text(
                text = album?.title.orEmpty(),
                color = colorPalette.text,
                style = typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            IconButton(
                onClick = { viewModel.toggleLove() }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (uiState.isLoved) R.drawable.heart else R.drawable.heart_outline
                    ),
                    contentDescription = if (uiState.isLoved) "Unlike" else "Like",
                    tint = (if (uiState.isLoved) colorPalette.accent else colorPalette.text),
                    modifier = Modifier.size(24.dp)
                )
            }

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
                        text = stringResource(id = R.string.view_artist),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    album?.artistId?.let { onGoToArtist(it) }
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
        isLoading = uiState.isLoading,
        thumbnailUrl = album?.thumbnailUrl,
        headerTitle = album?.title.orEmpty(),
        content = {
            AlbumSongs(
                browseId = browseId,
                onGoToArtist = onGoToArtist
            )
        },
        onBackClick = onBack,
    )
}