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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.ui.components.AdaptiveThumbnail
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.viewmodels.AlbumViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun NewAlbumScreen(
    browseId: String,
    onGoToArtist: (String) -> Unit,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: AlbumViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(browseId) {
        viewModel.initAlbum(browseId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val album = uiState.album
    val (colorPalette) = LocalAppearance.current

    SettingsScreenLayout(
        title = {},
        scrollable = false,
        horizontalPadding = 0.dp,
        onBackClick = onBack,
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
        }
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            AdaptiveThumbnail(
                isLoading = uiState.isLoading,
                url = album?.thumbnailUrl,
                modifier = Modifier.fillMaxWidth(0.55f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = album?.title.orEmpty(),
                style = typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = colorPalette.accent,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album?.authorsText.orEmpty(),
                style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colorPalette.text,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .fillMaxWidth(0.8f)
                    .basicMarquee()
                    .clickable(
                        enabled = album?.artistId != null,
                        onClick = {
                            album?.artistId?.let { onGoToArtist(it) }
                        }
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            album?.year?.let {
                Text(
                    text = it,
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorPalette.text,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard(
            shape = RoundedCornerShape(
                topStart = 25.dp,
                topEnd = 25.dp
            )
        ) {
            NewAlbumSongs(
                browseId = browseId,
                onGoToArtist = onGoToArtist
            )
        }
    }
}