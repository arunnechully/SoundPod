package com.github.soundpod.ui.screens.album

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.ui.components.AdaptiveThumbnail
import com.github.soundpod.ui.components.PlaylistScreenLayout
import com.github.soundpod.ui.modifier.fadingEdge
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


    val fadingEdge = Brush.horizontalGradient(
        0f to Color.Transparent,
        0.1f to Color.Black,
        0.9f to Color.Black,
        1f to Color.Transparent
    )

    PlaylistScreenLayout(
        onBackClick = onBack,
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                AdaptiveThumbnail(
                    isLoading = uiState.isLoading,
                    url = album?.thumbnailUrl,
                    modifier = Modifier.fillMaxWidth(0.55f)
                )
                Text(
                    text = album?.title.orEmpty(),
                    style = typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = colorPalette.text,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
                Text(
                    text = album?.authorsText.orEmpty(),
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorPalette.accent,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = album?.artistId != null,
                            onClick = {
                                album?.artistId?.let { onGoToArtist(it) }
                            }
                        )
                        .fadingEdge(fadingEdge)
                        .basicMarquee()
                        .padding(horizontal = 8.dp, vertical = 2.dp)

                )
            }
        },
        content = {
            AlbumSongs(
                browseId = browseId,
                onGoToArtist = onGoToArtist
            )
        }
    )
}