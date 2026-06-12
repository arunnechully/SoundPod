package com.github.soundpod.ui.screens.playlist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.innertube.Innertube
import com.github.innertube.requests.playlistPage
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.Playlist
import com.github.soundpod.models.SongPlaylistMap
import com.github.soundpod.query
import com.github.soundpod.transaction
import com.github.soundpod.ui.components.AdaptiveThumbnail
import com.github.soundpod.ui.components.PlaylistScreenLayout
import com.github.soundpod.ui.components.TextFieldDialog
import com.github.soundpod.utils.ScreenCache
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.completed
import com.github.soundpod.utils.isScreenCacheEnabledKey
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun PlaylistScreen(
    browseId: String,
    onBack: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    var playlistPage: Innertube.PlaylistOrAlbumPage? by remember(browseId) { mutableStateOf(null) }
    var isImportingPlaylist by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(browseId) {
        val isScreenCacheEnabled = context.preferences.getBoolean(isScreenCacheEnabledKey, true)
        val cacheKey = "playlist_$browseId"

        if (playlistPage == null && isScreenCacheEnabled) {
            playlistPage = ScreenCache.load(cacheKey)
        }

        withContext(Dispatchers.IO) {
            Innertube.playlistPage(browseId = browseId)
                ?.completed()
                ?.getOrNull()
                ?.let { page ->
                    withContext(Dispatchers.Main) {
                        playlistPage = page
                        if (isScreenCacheEnabled) {
                            ScreenCache.save(cacheKey, page)
                        }
                    }
                }
        }
    }

    BackHandler(onBack = onBack)

    PlaylistScreenLayout(
        onBackClick = onBack,
        title = {
            Text(
                text = playlistPage?.title.orEmpty(),
                color = colorPalette.text,
                style = typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            if (playlistPage != null) {
                IconButton(onClick = { isImportingPlaylist = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.heart_outline),
                        contentDescription = stringResource(R.string.add_to_playlist),
                        tint = colorPalette.text,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        dropDownMenuContent = { dismissMenu ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(id = R.string.share),
                        color = colorPalette.text,
                        style = typography.bodyLarge
                    )
                },
                onClick = {
                    val url = playlistPage?.url
                        ?: "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
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
//                    onSettingsClick()
                    dismissMenu()
                }
            )
        },
        headerContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AdaptiveThumbnail(
                    isLoading = playlistPage == null,
                    url = playlistPage?.thumbnail?.url,
                    modifier = Modifier.fillMaxWidth(0.55f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playlistPage?.title.orEmpty(),
                    style = typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = colorPalette.accent,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = playlistPage?.authors?.joinToString("") { it.name ?: "" }.orEmpty(),
                    style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorPalette.text,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                playlistPage?.year?.let {
                    Text(
                        text = it,
                        style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorPalette.text,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        content = {
            PlaylistSongs(
                playlistPage = playlistPage,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist
            )
        }
    )

    if (isImportingPlaylist) {
        TextFieldDialog(
            title = stringResource(id = R.string.import_playlist),
            hintText = stringResource(id = R.string.playlist_name_hint),
            initialTextInput = playlistPage?.title ?: "",
            onDismiss = { isImportingPlaylist = false },
            onDone = { text ->
                scope.launch(Dispatchers.IO) {
                    query {
                        transaction {
                            val playlistId = db.insert(
                                Playlist(
                                    name = text,
                                    browseId = browseId
                                )
                            )

                            playlistPage?.songsPage?.items
                                ?.map(Innertube.SongItem::asMediaItem)
                                ?.onEach(db::insert)
                                ?.mapIndexed { index, mediaItem ->
                                    SongPlaylistMap(
                                        songId = mediaItem.mediaId,
                                        playlistId = playlistId,
                                        position = index
                                    )
                                }?.let(db::insertSongPlaylistMaps)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        isImportingPlaylist = false
                    }
                }
            }
        )
    }
}
