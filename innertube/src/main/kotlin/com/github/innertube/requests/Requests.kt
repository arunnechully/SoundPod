package com.github.innertube.requests

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import com.github.innertube.Innertube
import com.github.innertube.models.*
import com.github.innertube.utils.*
import java.util.Locale
import java.io.IOException

// --- AlbumPage.kt ---

suspend fun Innertube.albumPage(browseId: String): Result<Innertube.PlaylistOrAlbumPage>? {
    return playlistPage(browseId = browseId)?.map { album ->
        album.url?.let { Url(it).parameters["list"] }?.let { playlistId ->
            playlistPage(browseId = "VL$playlistId")?.getOrNull()?.let { playlist ->
                album.copy(songsPage = playlist.songsPage)
            }
        } ?: album
    }?.map { album ->
        val albumInfo = Innertube.Info(
            name = Innertube.Info.cleanName(album.title),
            endpoint = NavigationEndpoint.Endpoint.Browse(browseId = browseId)
        )

        album.copy(
            songsPage = album.songsPage?.copy(
                items = album.songsPage.items?.map { song ->
                    song.copy(
                        authors = song.authors ?: album.authors,
                        album = albumInfo,
                        thumbnail = album.thumbnail
                    )
                }
            )
        )
    }
}

// --- ArtistPage.kt ---

suspend fun Innertube.artistPage(browseId: String): Result<Innertube.ArtistPage>? =
    runCatchingNonCancellable {
        val response = client.post(BROWSE) {
            setBody(
                BrowseBody(
                    localized = false,
                    browseId = browseId
                )
            )
            mask("contents,header")
        }.body<BrowseResponse>()

        val tabs = (response.contents?.singleColumnBrowseResultsRenderer?.tabs
            ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs)

        fun findSectionByTitle(text: String): SectionListRenderer.Content? {
            tabs?.forEach { tab ->
                tab.tabRenderer?.content?.sectionListRenderer?.findSectionByTitle(text)?.let { return it }
            }
            return null
        }

        val artistName = Innertube.Info.cleanName(response
            .header
            ?.musicImmersiveHeaderRenderer
            ?.title
            ?.text)

        val songsSection = (findSectionByTitle("Top songs")
            ?: findSectionByTitle("Songs"))
        val songsShelf = songsSection?.musicShelfRenderer
        val songsPlaylistShelf = songsSection?.musicPlaylistShelfRenderer

        val albumsSection = (findSectionByTitle("Albums")
            ?: findSectionByTitle("Discography"))?.musicCarouselShelfRenderer
        val singlesSection = (findSectionByTitle("Singles & EPs")
            ?: findSectionByTitle("Singles")
            ?: findSectionByTitle("Singles & albums"))?.musicCarouselShelfRenderer
        val playlistsSection = (findSectionByTitle("Playlists by $artistName")
            ?: findSectionByTitle("Playlists"))?.musicCarouselShelfRenderer
        val featuredPlaylistsSection = findSectionByTitle("Featured on")?.musicCarouselShelfRenderer
        val relatedArtistsSection =
            findSectionByTitle("Fans might also like")?.musicCarouselShelfRenderer

        val songsEndpoint = (songsShelf?.bottomEndpoint?.browseEndpoint
            ?: songsPlaylistShelf?.playlistId?.let { com.github.innertube.models.NavigationEndpoint.Endpoint.Browse(browseId = "VL$it") }
            ?: tabs?.mapNotNull { it.tabRenderer }
                ?.find { it.title?.equals("Songs", ignoreCase = true) == true || it.tabIdentifier == "FEmusic_library_songs" }
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull { it.musicShelfRenderer != null }
                ?.musicShelfRenderer
                ?.bottomEndpoint
                ?.browseEndpoint
        )

        Innertube.ArtistPage(
            name = artistName,
            description = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.description
                ?.text,
            thumbnail = (response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.foregroundThumbnail
                ?: response
                    .header
                    ?.musicImmersiveHeaderRenderer
                    ?.thumbnail)
                ?.musicThumbnailRenderer
                ?.thumbnail
                ?.thumbnails
                ?.getOrNull(0),
            shuffleEndpoint = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.playButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.watchEndpoint,
            radioEndpoint = response
                .header
                ?.musicImmersiveHeaderRenderer
                ?.startRadioButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.watchEndpoint,
            songs = songsShelf
                ?.contents
                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull { Innertube.SongItem.from(it) }
                ?: songsPlaylistShelf
                    ?.contents
                    ?.mapNotNull(MusicPlaylistShelfRenderer.Content::musicResponsiveListItemRenderer)
                    ?.mapNotNull { Innertube.SongItem.from(it) },
            songsEndpoint = songsEndpoint,
            albums = albumsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.AlbumItem::from),
            albumsEndpoint = albumsSection
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint,
            singles = singlesSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.AlbumItem::from),
            singlesEndpoint = singlesSection
                ?.header
                ?.musicCarouselShelfBasicHeaderRenderer
                ?.moreContentButton
                ?.buttonRenderer
                ?.navigationEndpoint
                ?.browseEndpoint,
            playlists = playlistsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.PlaylistItem::from),
            featuredPlaylists = featuredPlaylistsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.PlaylistItem::from),
            relatedArtists = relatedArtistsSection
                ?.contents
                ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                ?.mapNotNull(Innertube.ArtistItem::from)
        )
    }

// --- ExplorePage.kt ---

suspend fun Innertube.charts(): Result<List<Innertube.SongItem>?>? = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        waitForSession(timeoutMs = 10000)
    }

    suspend fun fetchCharts(browseId: String): List<Innertube.SongItem>? {
        val response = client.post(BROWSE) {
                setBody(
                BrowseBody(
                    browseId = browseId,
                    context = YouTubeClient.WEB_REMIX.toContext(
                        hl = "en",
                        gl = Locale.getDefault().country.ifBlank { "US" },
                    )
                )
            )
        }.body<BrowseResponse>()

        val sectionListRenderer = response
            .contents
            ?.sectionListRenderer

        return (sectionListRenderer?.findSectionByTitle("Top songs")
            ?: sectionListRenderer?.findSectionByTitle("Top music videos")
            ?: sectionListRenderer?.findSectionByTitle("Trending")
            ?: sectionListRenderer?.contents?.firstOrNull { it.musicCarouselShelfRenderer != null })
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from)
            ?.takeIf { it.isNotEmpty() }
    }

    fetchCharts("FEcharts") ?: fetchCharts("FEmusic_charts") ?: fetchCharts("FEmusic_home") ?: fetchCharts("FEmusic_explore")
}

// --- ItemsPage.kt ---

suspend fun <T : Innertube.Item> Innertube.itemsPage(
    browseId: String,
    params: String?,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null },
) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = browseId,
                params = params
            )
        )
    }.body<BrowseResponse>()

    val sectionListRenderer = (response.contents?.singleColumnBrowseResultsRenderer?.tabs
        ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs)
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer

    val shelves = sectionListRenderer?.contents
        ?.filter { it.musicShelfRenderer != null || it.musicPlaylistShelfRenderer != null || it.gridRenderer != null }
        ?: emptyList()

    if (shelves.isEmpty()) return@runCatchingNonCancellable null

    val items = mutableListOf<T>()
    var continuation: String? = sectionListRenderer?.continuations
        ?.firstOrNull()
        ?.nextContinuationData
        ?.continuation

    shelves.forEach { shelf ->
        val page = itemsPageFromMusicShelRendererOrGridRenderer(
            musicShelfRenderer = shelf.musicShelfRenderer,
            musicPlaylistShelfRenderer = shelf.musicPlaylistShelfRenderer,
            gridRenderer = shelf.gridRenderer,
            fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
            fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer,
        )
        page?.items?.let { items.addAll(it) }
        if (continuation == null) continuation = page?.continuation
    }

    Innertube.ItemsPage(
        items = items.ifEmpty { null },
        continuation = continuation
    )
}

suspend fun <T : Innertube.Item> Innertube.itemsPageContinuation(
    continuation: String,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T? = { null },
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T? = { null },
) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(ContinuationBody(continuation = continuation))
    }.body<ContinuationResponse>()

    val items = mutableListOf<T>()
    var nextContinuation: String? = null

    response.continuationContents?.let { contents ->
        contents.musicShelfContinuation?.let { shelf ->
            val page = itemsPageFromMusicShelRendererOrGridRenderer(
                musicShelfRenderer = shelf,
                musicPlaylistShelfRenderer = null,
                gridRenderer = null,
                fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
                fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer,
            )
            page?.items?.let { items.addAll(it) }
            nextContinuation = page?.continuation
        }

        contents.sectionListContinuation?.let { sectionList ->
            nextContinuation = sectionList.continuations
                ?.firstOrNull()
                ?.nextContinuationData
                ?.continuation

            sectionList.contents?.forEach { shelf ->
                val page = itemsPageFromMusicShelRendererOrGridRenderer(
                    musicShelfRenderer = shelf.musicShelfRenderer,
                    musicPlaylistShelfRenderer = shelf.musicPlaylistShelfRenderer,
                    gridRenderer = shelf.gridRenderer,
                    fromMusicResponsiveListItemRenderer = fromMusicResponsiveListItemRenderer,
                    fromMusicTwoRowItemRenderer = fromMusicTwoRowItemRenderer,
                )
                page?.items?.let { items.addAll(it) }
                if (nextContinuation == null) nextContinuation = page?.continuation
            }
        }
    }

    Innertube.ItemsPage(
        items = items.ifEmpty { null },
        continuation = nextContinuation
    )
}

private fun <T : Innertube.Item> itemsPageFromMusicShelRendererOrGridRenderer(
    musicShelfRenderer: MusicShelfRenderer?,
    musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer? = null,
    gridRenderer: GridRenderer?,
    fromMusicResponsiveListItemRenderer: (MusicResponsiveListItemRenderer) -> T?,
    fromMusicTwoRowItemRenderer: (MusicTwoRowItemRenderer) -> T?,
): Innertube.ItemsPage<T>? {
    return if (musicShelfRenderer != null) {
        Innertube.ItemsPage(
            continuation = musicShelfRenderer
                .continuations
                ?.firstOrNull()
                ?.nextContinuationData
                ?.continuation,
            items = musicShelfRenderer
                .contents
                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull(fromMusicResponsiveListItemRenderer)
        )
    } else if (musicPlaylistShelfRenderer != null) {
        Innertube.ItemsPage(
            continuation = musicPlaylistShelfRenderer
                .continuations
                ?.firstOrNull()
                ?.nextContinuationData
                ?.continuation,
            items = musicPlaylistShelfRenderer
                .contents
                ?.mapNotNull(MusicPlaylistShelfRenderer.Content::musicResponsiveListItemRenderer)
                ?.mapNotNull(fromMusicResponsiveListItemRenderer)
        )
    } else if (gridRenderer != null) {
        Innertube.ItemsPage(
            continuation = null,
            items = gridRenderer
                .items
                ?.mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                ?.mapNotNull(fromMusicTwoRowItemRenderer)
        )
    } else null
}

// --- Lyrics.kt ---

suspend fun Innertube.lyrics(videoId: String): Result<String?>? = runCatchingNonCancellable {
    val nextResponse = client.post(NEXT) {
        setBody(NextBody(videoId = videoId))
        mask("contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer(endpoint,title)")
    }.body<NextResponse>()

    val browseId = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.getOrNull(1)
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId
        ?: return@runCatchingNonCancellable null

    val response = client.post(BROWSE) {
        setBody(BrowseBody(browseId = browseId))
        mask("contents.sectionListRenderer.contents.musicDescriptionShelfRenderer.description")
    }.body<BrowseResponse>()

    response.contents
        ?.sectionListRenderer
        ?.contents
        ?.firstOrNull()
        ?.musicDescriptionShelfRenderer
        ?.description
        ?.text
}

// --- NextPage.kt ---

suspend fun Innertube.nextPage(
    videoId: String? = null,
    playlistId: String? = null,
    params: String? = null,
    playlistSetVideoId: String? = null
): Result<Innertube.NextPage>? =
    runCatchingNonCancellable {
        val response = client.post(NEXT) {
            setBody(
                NextBody(
                    videoId = videoId,
                    playlistId = playlistId,
                    params = params,
                    playlistSetVideoId = playlistSetVideoId
                )
            )
            mask("contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.tabRenderer.content.musicQueueRenderer.content.playlistPanelRenderer(continuations,contents(automixPreviewVideoRenderer,$PLAYLIST_PANEL_VIDEO_RENDERER_MASK))")
        }.body<NextResponse>()

        val tabs = response
            .contents
            ?.singleColumnMusicWatchNextResultsRenderer
            ?.tabbedRenderer
            ?.watchNextTabbedResultsRenderer
            ?.tabs

        val playlistPanelRenderer = tabs
            ?.getOrNull(0)
            ?.tabRenderer
            ?.content
            ?.musicQueueRenderer
            ?.content
            ?.playlistPanelRenderer

        if (playlistId == null) {
            val endpoint = playlistPanelRenderer
                ?.contents
                ?.lastOrNull()
                ?.automixPreviewVideoRenderer
                ?.content
                ?.automixPlaylistVideoRenderer
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint

            if (endpoint != null) {
                return nextPage(
                    videoId = videoId,
                    playlistId = endpoint.playlistId,
                    params = endpoint.params,
                    playlistSetVideoId = playlistSetVideoId
                )
            }
        }

        Innertube.NextPage(
            playlistId = playlistId,
            playlistSetVideoId = playlistSetVideoId,
            params = params,
            itemsPage = playlistPanelRenderer
                ?.toSongsPage()
        )
    }

suspend fun Innertube.nextPage(continuation: String) = runCatchingNonCancellable {
    val response = client.post(NEXT) {
        setBody(ContinuationBody(continuation = continuation))
        mask("continuationContents.playlistPanelContinuation(continuations,contents.$PLAYLIST_PANEL_VIDEO_RENDERER_MASK)")
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.playlistPanelContinuation
        ?.toSongsPage()
}

private fun NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer?.toSongsPage() =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull(NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer.Content::playlistPanelVideoRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )

// --- Player.kt ---

data class PlayerResult(
    val response: PlayerResponse,
    val userAgent: String
)

suspend fun Innertube.player(videoId: String): Result<PlayerResult>? = runCatchingNonCancellable {
    waitForSession(5000)

    val vrResponse = tryPlayer(videoId, YouTubeClient.ANDROID_VR)
    if (vrResponse?.playabilityStatus?.status == "OK") {
        println("Innertube: Successfully used ANDROID_VR client for $videoId")
        return@runCatchingNonCancellable PlayerResult(
            response = vrResponse.applyDecipher(decipher, signatureDecipher),
            userAgent = YouTubeClient.ANDROID_VR.userAgent
        )
    }

    throw Exception("All Innertube player clients failed for $videoId")
}

private suspend fun Innertube.tryPlayer(
    videoId: String, 
    clientType: YouTubeClient, 
    extraHeaders: Map<String, String> = emptyMap(),
    includeThirdParty: Boolean = false,
    host: String = "www.youtube.com"
): PlayerResponse? = runCatching {
    client.post("https://$host/youtubei/v1/player") {
        header("User-Agent", clientType.userAgent)
        extraHeaders.forEach { (key, value) -> header(key, value) }
        
        setBody(
            PlayerBody(
                context = clientType.toContext(visitorData = visitorData, includeThirdParty = includeThirdParty),
                videoId = videoId,
                serviceIntegrityDimensions = poToken?.let { ServiceIntegrityDimensions(poToken = it) }
            )
        )
        mask("playabilityStatus(status,reason,messages),playerConfig.audioConfig,streamingData.adaptiveFormats,streamingData.formats,videoDetails.videoId")
    }.body<PlayerResponse>()
}.getOrNull()

private suspend fun PlayerResponse.applyDecipher(
    decipherN: (suspend (String) -> String)?,
    decipherSig: (suspend (String) -> String)?
): PlayerResponse {
    if (streamingData == null) return this
    
    return copy(
        streamingData = streamingData.copy(
            adaptiveFormats = streamingData.adaptiveFormats?.map { format ->
                val url = format.url ?: format.signatureCipher?.let { parseSignatureCipher(it, decipherSig) }
                format.copy(url = url?.let { decipherUrl(it, decipherN) })
            },
            formats = streamingData.formats?.map { format ->
                val url = format.url ?: format.signatureCipher?.let { parseSignatureCipher(it, decipherSig) }
                format.copy(url = url?.let { decipherUrl(it, decipherN) })
            }
        )
    )
}

private suspend fun parseSignatureCipher(cipher: String, decipher: (suspend (String) -> String)?): String? {
    val params = cipher.split("&").associate { 
        val parts = it.split("=")
        parts[0] to java.net.URLDecoder.decode(parts[1], "UTF-8")
    }
    
    val baseUrl = params["url"] ?: return null
    val signature = params["s"] ?: return baseUrl
    val sp = params["sp"] ?: "sig"
    
    val decipheredSig = if (decipher != null) decipher(signature) else signature
    
    return if (baseUrl.contains("?")) {
        "$baseUrl&$sp=$decipheredSig"
    } else {
        "$baseUrl?$sp=$decipheredSig"
    }
}

private suspend fun decipherUrl(url: String, decipher: (suspend (String) -> String)?): String {
    if (decipher == null) return url
    val nParam = url.substringAfter("&n=", "").substringBefore("&")
    if (nParam.isEmpty()) return url
    
    val decipheredN = decipher(nParam)
    return url.replace("&n=$nParam", "&n=$decipheredN")
}

// --- PlaylistPage.kt ---

suspend fun Innertube.playlistPage(
    browseId: String,
    params: String? = null
) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = browseId,
                params = params
            )
        )
    }.body<BrowseResponse>()

    val header = response
        .contents
        ?.twoColumnBrowseResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.firstOrNull()
        ?.musicResponsiveHeaderRenderer

    val contents = response
        .contents
        ?.twoColumnBrowseResultsRenderer
        ?.secondaryContents
        ?.sectionListRenderer
        ?.contents

    // Standard playlists (PL/VL)
    val musicShelfRenderer = contents
        ?.firstOrNull()
        ?.musicShelfRenderer

    // Mixes and Charts (RDCLAK/Mixes)
    val musicPlaylistShelfRenderer = contents
        ?.firstOrNull()
        ?.musicPlaylistShelfRenderer

    val otherVersionsSection = if (contents?.size == 3) contents.getOrNull(1)
    else {
        val section = contents?.getOrNull(1)
        if (section?.musicCarouselShelfRenderer?.contents?.size == 10) null
        else section
    }

    val relatedAlbumsSection = if (contents?.size == 3) contents.getOrNull(2)
    else {
        val section = contents?.getOrNull(1)
        if (section?.musicCarouselShelfRenderer?.contents?.size == 10) section
        else null
    }

    Innertube.PlaylistOrAlbumPage(
        title = Innertube.Info.cleanName(header
            ?.title
            ?.text),
        thumbnail = header
            ?.thumbnail
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.firstOrNull(),
        authors = header
            ?.straplineTextOne
            ?.splitBySeparator()
            ?.getOrNull(0)
            ?.map(Innertube::Info),
        year = header
            ?.subtitle
            ?.splitBySeparator()
            ?.getOrNull(1)
            ?.firstOrNull()
            ?.text,
        url = response
            .microformat
            ?.microformatDataRenderer
            ?.urlCanonical,
        songsPage = musicShelfRenderer?.toSongsPage()
            ?: musicPlaylistShelfRenderer?.toSongsPage(),
        otherVersions = otherVersionsSection
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        relatedAlbums = relatedAlbumsSection
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from)
    )
}

suspend fun Innertube.playlistPageContinuation(continuation: String) = runCatchingNonCancellable {
    val response = client.post(BROWSE) {
        setBody(ContinuationBody(continuation = continuation))
        mask("continuationContents.musicPlaylistShelfContinuation(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)")
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toSongsPage()
}

/**
 * Standard Shelf Converter (PL/VL)
 */
private fun MusicShelfRenderer?.toSongsPage() =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )

/**
 * Mix/Chart Shelf Converter (RDCLAK)
 */
private fun MusicPlaylistShelfRenderer?.toSongsPage() =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull { it.musicResponsiveListItemRenderer }
            ?.mapNotNull(Innertube.SongItem::from),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )

// --- Queue.kt ---

suspend fun Innertube.queue(videoIds: List<String>) = runCatchingNonCancellable {
    val response = client.post(QUEUE) {
        setBody(QueueBody(videoIds = videoIds))
        mask("queueDatas.content.$PLAYLIST_PANEL_VIDEO_RENDERER_MASK")
    }.body<GetQueueResponse>()

    response
        .queueDatas
        ?.mapNotNull { queueData ->
            queueData
                .content
                ?.playlistPanelVideoRenderer
                ?.let(Innertube.SongItem::from)
        }
}

suspend fun Innertube.song(videoId: String): Result<Innertube.SongItem?>? =
    queue(videoIds = listOf(videoId))?.map { it?.firstOrNull() }

// --- Recommendations.kt ---

suspend fun Innertube.recommendations(): Result<List<Innertube.SongItem>?>? = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        waitForSession(timeoutMs = 10000)
    }

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = "FEmusic_home",
                context = YouTubeClient.WEB_REMIX.toContext(
                    gl = Locale.getDefault().country.ifBlank { "US" },
                    visitorData = visitorData
                )
            )
        )
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    (sectionListRenderer?.findSectionByTitle("Quick picks")
        ?: sectionListRenderer?.findSectionByTitle("Recommended")
        ?: sectionListRenderer?.findSectionByTitle("Listen again")
        ?: sectionListRenderer?.contents?.firstOrNull { it.musicCarouselShelfRenderer != null })
        ?.musicCarouselShelfRenderer
        ?.contents
        ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
        ?.mapNotNull(Innertube.SongItem::from)
        ?.takeIf { it.isNotEmpty() }
}

// --- RelatedPage.kt ---

suspend fun Innertube.relatedPage(videoId: String) = runCatchingNonCancellable {
    if (!hasRequiredTokens) {
        waitForSession(timeoutMs = 10000)
    }

    val nextResponse = client.post(NEXT) {
        setBody(NextBody(videoId = videoId))
    }.body<NextResponse>()

    val browseId = nextResponse
        .contents
        ?.singleColumnMusicWatchNextResultsRenderer
        ?.tabbedRenderer
        ?.watchNextTabbedResultsRenderer
        ?.tabs
        ?.let { tabs ->
            tabs.find { it.tabRenderer?.endpoint?.browseEndpoint?.browseId?.startsWith("FEmusic_related") == true }
                ?: tabs.find { it.tabRenderer?.title == "Related" }
                ?: tabs.getOrNull(2)
        }
        ?.tabRenderer
        ?.endpoint
        ?.browseEndpoint
        ?.browseId
        ?: return@runCatchingNonCancellable null

    val response = client.post(BROWSE) {
        setBody(
            BrowseBody(
                browseId = browseId,
                context = YouTubeClient.WEB_REMIX.toContext(
                    hl = "en",
                    gl = Locale.getDefault().country.ifBlank { "US" },
                )
            )
        )
    }.body<BrowseResponse>()

    val sectionListRenderer = response
        .contents
        ?.sectionListRenderer

    Innertube.RelatedPage(
        songs = sectionListRenderer
            ?.findSectionByTitle("You might also like", "Related", "More from", "Similar")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicResponsiveListItemRenderer)
            ?.mapNotNull(Innertube.SongItem::from),
        playlists = sectionListRenderer
            ?.findSectionByTitle("Recommended playlists", "Playlists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.PlaylistItem::from)
            ?.sortedByDescending { it.channel?.name == "YouTube Music" },
        albums = sectionListRenderer
            ?.findSectionByStrapline("MORE FROM")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.AlbumItem::from),
        artists = sectionListRenderer
            ?.findSectionByTitle("Similar artists", "Artists")
            ?.musicCarouselShelfRenderer
            ?.contents
            ?.mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
            ?.mapNotNull(Innertube.ArtistItem::from),
    )
}

// --- SearchPage.kt ---

suspend fun <T : Innertube.Item> Innertube.searchPage(
    query: String,
    params: String,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingNonCancellable {
    val response = client.post(SEARCH) {
        setBody(
            SearchBody(
                query = query,
                params = params
            )
        )
        mask("contents.tabbedSearchResultsRenderer.tabs.tabRenderer.content.sectionListRenderer.contents.musicShelfRenderer(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)")
    }.body<SearchResponse>()

    response
        .contents
        ?.tabbedSearchResultsRenderer
        ?.tabs
        ?.firstOrNull()
        ?.tabRenderer
        ?.content
        ?.sectionListRenderer
        ?.contents
        ?.lastOrNull()
        ?.musicShelfRenderer
        ?.toItemsPage(fromMusicShelfRendererContent)
}

suspend fun <T : Innertube.Item> Innertube.searchPage(
    continuation: String,
    fromMusicShelfRendererContent: (MusicShelfRenderer.Content) -> T?
) = runCatchingNonCancellable {
    val response = client.post(SEARCH) {
        setBody(ContinuationBody(continuation = continuation))
        mask("continuationContents.musicShelfContinuation(continuations,contents.$MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK)")
    }.body<ContinuationResponse>()

    response
        .continuationContents
        ?.musicShelfContinuation
        ?.toItemsPage(fromMusicShelfRendererContent)
}

private fun <T : Innertube.Item> MusicShelfRenderer?.toItemsPage(mapper: (MusicShelfRenderer.Content) -> T?) =
    Innertube.ItemsPage(
        items = this
            ?.contents
            ?.mapNotNull(mapper),
        continuation = this
            ?.continuations
            ?.firstOrNull()
            ?.nextContinuationData
            ?.continuation
    )

// --- SearchSuggestions.kt ---

suspend fun Innertube.searchSuggestions(input: String) = runCatchingNonCancellable {
    val response = client.post(SEARCH_SUGGESTIONS) {
        setBody(SearchSuggestionsBody(input = input))
        mask("contents.searchSuggestionsSectionRenderer.contents.searchSuggestionRenderer.navigationEndpoint.searchEndpoint.query")
    }.body<SearchSuggestionsResponse>()

    response
        .contents
        ?.firstOrNull()
        ?.searchSuggestionsSectionRenderer
        ?.contents
        ?.mapNotNull { content ->
            content
                .searchSuggestionRenderer
                ?.navigationEndpoint
                ?.searchEndpoint
                ?.query
        }
}
