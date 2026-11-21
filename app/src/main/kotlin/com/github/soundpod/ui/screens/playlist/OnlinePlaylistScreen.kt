package com.github.soundpod.ui.screens.playlist

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.github.core.ui.LocalAppearance
import com.github.innertube.Innertube
import com.github.innertube.models.Thumbnail
import com.github.innertube.requests.playlistPage
import com.github.innertube.requests.playlistPageContinuation
import com.github.innertube.utils.plus
import com.github.soundcore.Soundcore
import com.github.soundcore.asSoundcoreSongItem
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.Playlist
import com.github.soundpod.models.SongPlaylistMap
import com.github.soundpod.query
import com.github.soundpod.transaction
import com.github.soundpod.ui.components.ParallaxScreenLayout
import com.github.soundpod.ui.components.TextFieldDialog
import com.github.soundpod.utils.ScreenCache
import com.github.soundpod.utils.asMediaItem
import com.github.soundpod.utils.completed
import com.github.soundpod.utils.isScreenCacheEnabledKey
import com.github.soundpod.utils.preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun OnlinePlaylistScreen(
    browseId: String,
    onBack: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit,
) {
    val context = LocalContext.current
    val (colorPalette) = LocalAppearance.current
    var playlistPage: Innertube.PlaylistOrAlbumPage? by remember(browseId) { mutableStateOf(null) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var localPlaylist: Playlist? by remember { mutableStateOf(null) }
    var isImportingPlaylist by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(browseId) {
        db.playlist(browseId).collect { localPlaylist = it }
    }

    LaunchedEffect(browseId) {
        val isScreenCacheEnabled = context.preferences.getBoolean(isScreenCacheEnabledKey, true)
        val cacheKey = "playlist_$browseId"

        if (playlistPage == null && isScreenCacheEnabled) {
            playlistPage = ScreenCache.load(cacheKey)
        }

        if (browseId.startsWith("soundcore_playlist:") || browseId.startsWith("saavn_playlist:") || browseId.startsWith("soundcore:") || browseId.startsWith("saavn:")) {
            val playlistId = browseId
                .removePrefix("soundcore_playlist:")
                .removePrefix("saavn_playlist:")
                .removePrefix("soundcore:")
                .removePrefix("saavn:")
            withContext(Dispatchers.IO) {
                val soundcorePlaylist = Soundcore.getPlaylistDetails(playlistId)
                if (soundcorePlaylist != null) {
                    val innertubeSongs = soundcorePlaylist.songs.map { it.asSoundcoreSongItem }
                    val page = Innertube.PlaylistOrAlbumPage(
                        title = soundcorePlaylist.title,
                        authors = soundcorePlaylist.artistsText?.let { listOf(Innertube.Info(name = it, endpoint = null)) },
                        year = soundcorePlaylist.year,
                        thumbnail = soundcorePlaylist.thumbnailUrl?.let { Thumbnail(url = it, width = 500, height = 500) },
                        url = "https://www.jiosaavn.com/featured/$playlistId",
                        songsPage = Innertube.ItemsPage(items = innertubeSongs, continuation = null),
                        otherVersions = emptyList(),
                        relatedAlbums = emptyList()
                    )
                    withContext(Dispatchers.Main) {
                        playlistPage = page
                    }
                    if (isScreenCacheEnabled) {
                        ScreenCache.save(cacheKey, page)
                    }
                }
            }
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            var page = Innertube.playlistPage(browseId = browseId)?.getOrNull()
            if (page != null) {
                withContext(Dispatchers.Main) {
                    playlistPage = page
                }
                while (page?.songsPage?.continuation != null) {
                    withContext(Dispatchers.Main) {
                        isLoadingMore = true
                    }
                    val continuation = page.songsPage?.continuation!!
                    val nextContinuation = Innertube.playlistPageContinuation(continuation)?.getOrNull() ?: break
                    val currentSongsPage = page.songsPage
                    val updatedSongsPage = currentSongsPage + nextContinuation
                    page = page.copy(songsPage = updatedSongsPage)
                    withContext(Dispatchers.Main) {
                        playlistPage = page
                    }
                }
                withContext(Dispatchers.Main) {
                    isLoadingMore = false
                }
                if (isScreenCacheEnabled && page != null) {
                    ScreenCache.save(cacheKey, page)
                }
            }
        }
    }

    val loadMore: () -> Unit = {
        val continuation = playlistPage?.songsPage?.continuation
        if (continuation != null && !isLoadingMore) {
            isLoadingMore = true
            scope.launch(Dispatchers.IO) {
                val continuationResult = Innertube.playlistPageContinuation(continuation)?.getOrNull()
                withContext(Dispatchers.Main) {
                    if (continuationResult != null) {
                        val currentSongsPage = playlistPage?.songsPage
                        val updatedSongsPage = currentSongsPage + continuationResult
                        val updatedPage = playlistPage?.copy(songsPage = updatedSongsPage)
                        playlistPage = updatedPage
                        if (context.preferences.getBoolean(isScreenCacheEnabledKey, true) && updatedPage != null) {
                            ScreenCache.save("playlist_$browseId", updatedPage)
                        }
                    }
                    isLoadingMore = false
                }
            }
        }
    }

    BackHandler(onBack = onBack)

    ParallaxScreenLayout(
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
                val isBookmarked = localPlaylist != null
                IconButton(
                    onClick = {
                        if (isBookmarked) {
                            query { localPlaylist?.let(db::delete) }
                        } else {
                            isImportingPlaylist = true
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isBookmarked) R.drawable.heart else R.drawable.heart_outline
                        ),
                        contentDescription = stringResource(
                            if (isBookmarked) R.string.remove_from_favorites else R.string.add_to_playlist
                        ),
                        tint = if (isBookmarked) colorPalette.accent else colorPalette.text,
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
                        ?: if (browseId.startsWith("soundcore") || browseId.startsWith("saavn")) {
                            "https://www.jiosaavn.com/featured/${browseId.substringAfter(":")}"
                        } else {
                            "https://music.youtube.com/playlist?list=${browseId.removePrefix("VL")}"
                        }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                    dismissMenu()
                }
            )
        },
        isLoading = playlistPage == null,
        thumbnailUrl = playlistPage?.thumbnail?.url,
        headerTitle = playlistPage?.title.orEmpty(),
        content = {
            OnlinePlaylistSongs(
                playlistPage = playlistPage,
                isLoadingMore = isLoadingMore,
                onLoadMore = loadMore,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist
            )
        },
        onBackClick = onBack,
    )

    if (isImportingPlaylist) {
        TextFieldDialog(
            title = stringResource(id = R.string.import_playlist),
            hintText = stringResource(id = R.string.playlist_name_hint),
            initialTextInput = playlistPage?.title ?: "",
            onDismiss = { isImportingPlaylist = false },
            onDone = { text ->
                scope.launch(Dispatchers.IO) {
                    val fullPage = playlistPage?.let { page ->
                        Result.success(page).completed()?.getOrNull() ?: page
                    } ?: playlistPage
                    query {
                        transaction {
                            val playlistId = db.insert(
                                Playlist(
                                    name = text,
                                    browseId = browseId
                                )
                            )

                            fullPage?.songsPage?.items
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
