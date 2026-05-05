package com.github.soundpod.ui.screens.album

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.models.Section
import com.github.soundpod.ui.components.AdaptiveThumbnail
import com.github.soundpod.utils.thumbnail
import com.github.soundpod.viewmodels.AlbumViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun NewAlbumScreen(
    browseId: String,
    onGoToArtist: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AlbumViewModel = viewModel()
) {
    BackHandler { onBack() }

    LaunchedEffect(browseId) {
        viewModel.initAlbum(browseId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val album = uiState.album

    val(colorPalette) = LocalAppearance.current
    val windowInfo = LocalWindowInfo.current

    val (blurRadius, cornerRadius) = remember(windowInfo.containerSize) {
        val blur = (windowInfo.containerSize.height * 0.15f).dp
        val corner = (windowInfo.containerSize.width * 0.08f).dp.coerceAtMost(32.dp)
        blur to corner
    }

    val tabs = listOf(
        Section(stringResource(id = R.string.songs), Icons.Outlined.MusicNote),
        Section(stringResource(id = R.string.other_versions), Icons.Outlined.Album),
        Section(stringResource(id = R.string.related_albums), Icons.Outlined.AutoAwesome)
    )

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onTabSelected(pagerState.currentPage)
    }

    val lowResForBlurUrl = remember(album?.thumbnailUrl) {
        album?.thumbnailUrl?.thumbnail(10)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette.background4)
    ) {
        // Blurred Background Image
        AsyncImage(
            model = lowResForBlurUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = 0.99f
                }
                .blur(blurRadius)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(colorPalette.background3.copy(alpha = 0.35f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.arrow_back),
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(colorPalette.text)
                )
            }

            IconButton(
                onClick = { viewModel.toggleLove() }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (uiState.isLoved) R.drawable.heart else R.drawable.heart_outline
                    ),
                    contentDescription = if (uiState.isLoved) "Unlike" else "Like",
                    tint = (if (uiState.isLoved) Color.Red else colorPalette.text),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            AdaptiveThumbnail(
                isLoading = uiState.isLoading,
                url = album?.thumbnailUrl,
                modifier = Modifier.fillMaxWidth(0.55f)
            )

            Text(
                text = album?.title.orEmpty(),
                style = typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = colorPalette.text,
                textAlign = TextAlign.Center
            )

            Text(
                text = album?.authorsText.orEmpty(),
                style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colorPalette.text,
                textAlign = TextAlign.Center
            )

            album?.year?.let {
                Text(
                    text = it,
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorPalette.text,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.53f)
                .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
                .background(colorPalette.baseColor)
        ) {
            NewAlbumSongs(
                browseId = browseId,
                onGoToArtist = onGoToArtist
            )
        }
    }
}