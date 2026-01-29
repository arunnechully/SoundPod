package com.github.soundpod.ui.screens.album

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.core.ui.LocalAppearance
import com.github.innertube.Innertube
import com.github.innertube.requests.albumPage
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.Album
import com.github.soundpod.models.Section
import com.github.soundpod.models.SongAlbumMap
import com.github.soundpod.ui.components.adaptiveThumbnailContent
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun TodoAlbumScreen(
    browseId: String,
    pop: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val(colorPalette) = LocalAppearance.current

    var album: Album? by remember { mutableStateOf(null) }
    var albumPage: Innertube.PlaylistOrAlbumPage? by remember { mutableStateOf(null) }

    val isLoved = false
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

    val thumbnailContent =
        adaptiveThumbnailContent(album?.timestamp == null, album?.thumbnailUrl)

    val thumbnailUrl = album?.thumbnailUrl

    LaunchedEffect(Unit) {
        db
            .album(browseId)
            .combine(snapshotFlow { pagerState.currentPage }) { album, tabIndex -> album to tabIndex }
            .collect { (currentAlbum, tabIndex) ->
                album = currentAlbum

                if (albumPage == null && (currentAlbum?.timestamp == null || tabIndex >= 1)) {
                    withContext(Dispatchers.IO) {
                        Innertube.albumPage(browseId = browseId)
                            ?.completed()
                            ?.onSuccess { currentAlbumPage ->
                                albumPage = currentAlbumPage

                                db.clearAlbum(browseId)

                                db.upsert(
                                    Album(
                                        id = browseId,
                                        title = currentAlbumPage.title,
                                        thumbnailUrl = currentAlbumPage.thumbnail?.url,
                                        year = currentAlbumPage.year,
                                        authorsText = currentAlbumPage.authors
                                            ?.joinToString("") { it.name ?: "" },
                                        shareUrl = currentAlbumPage.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = album?.bookmarkedAt
                                    ),
                                    currentAlbumPage
                                        .songsPage
                                        ?.items
                                        ?.map(Innertube.SongItem::asMediaItem)
                                        ?.onEach(db::insert)
                                        ?.mapIndexed { position, mediaItem ->
                                            SongAlbumMap(
                                                songId = mediaItem.mediaId,
                                                albumId = browseId,
                                                position = position
                                            )
                                        } ?: emptyList()
                                )
                            }
                    }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorPalette.background4)
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
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
                    painter = painterResource(id = R.drawable.chevron_back),
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(colorPalette.text)
                )
            }

            IconButton(
                onClick = {}
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        if (isLoved) R.drawable.heart else R.drawable.heart_outline
                    ),
                    contentDescription = if (isLoved) "Unlike" else "Like",
                    tint = (if (isLoved) Color.Red else colorPalette.text),
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
            AsyncImage(
                model = thumbnailUrl,
                placeholder = painterResource(id = R.drawable.app_icon),
                error = painterResource(id = R.drawable.app_icon),
                fallback = painterResource(id = R.drawable.app_icon),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.45f)
//                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(cornerRadius / 2))
            )

            Spacer(modifier = Modifier.padding(vertical = 5.dp))

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

        // Song list
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.53f)
                .clip(
                    RoundedCornerShape(
                        topStart = cornerRadius,
                        topEnd = cornerRadius
                    )
                )
                .background(colorPalette.baseColor)
        ) {
            AlbumSongs(
                browseId = browseId,
                thumbnailContent = thumbnailContent,
                onGoToArtist = onGoToArtist
            )
        }
    }
}