@file:Suppress("SpellCheckingInspection")

package com.github.innertube

import com.github.innertube.models.NavigationEndpoint
import com.github.innertube.models.Runs
import com.github.innertube.models.Thumbnail
import com.github.innertube.models.YouTubeClient
import com.github.innertube.utils.brotli
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object Innertube {

    var visitorData: String? = null
        set(value) {
            field = value
            onVisitorDataChanged?.invoke(value)
        }

    var onVisitorDataChanged: ((String?) -> Unit)? = null
    var poToken: String? = null
    var cookies: String? = null
    var decipher: (suspend (String) -> String)? = null

    interface PoTokenResolver {
        suspend fun getPoToken(videoId: String?): String?
    }

    var poTokenResolver: PoTokenResolver? = null

    val client = HttpClient(OkHttp) {
        expectSuccess = true

        install(HttpTimeout) {
            requestTimeoutMillis = 30.seconds.inWholeMilliseconds
            connectTimeoutMillis = 30.seconds.inWholeMilliseconds
            socketTimeoutMillis = 30.seconds.inWholeMilliseconds
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }

        install(ContentEncoding) {
            brotli()
            gzip()
            deflate()
        }

        defaultRequest {
            url(scheme = "https", host ="music.youtube.com") {
                contentType(ContentType.Application.Json)
                headers.append("X-Goog-Api-Key", "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8")
                parameters.append("prettyPrint", "false")
            }
        }
    }

    @Serializable
    private data class VisitorDataResponse(
        val responseContext: ResponseContext
    ) {
        @Serializable
        data class ResponseContext(
            val visitorData: String
        )
    }

    suspend fun fetchVisitorData(): String? {
        return runCatching {
            client.post("https://music.youtube.com/youtubei/v1/music/get_search_suggestions") {
                setBody(mapOf("context" to YouTubeClient.WEB_REMIX.toContext(localized = false), "input" to ""))
            }.body<VisitorDataResponse>().responseContext.visitorData
        }.getOrNull()?.also {
            visitorData = it
        }
    }

    suspend fun waitForSession(timeoutMs: Long = 10000): Boolean {
        if (visitorData != null) return true
        
        return coroutineScope {
            val result = withTimeoutOrNull(timeoutMs.milliseconds) {
                if (visitorData == null) fetchVisitorData() else visitorData
            }
            result != null
        }
    }

    val hasRequiredTokens: Boolean
        get() = !visitorData.isNullOrBlank()

    internal const val BROWSE = "/youtubei/v1/browse"
    internal const val NEXT = "/youtubei/v1/next"
    internal const val PLAYER = "/youtubei/v1/player"
    internal const val QUEUE = "/youtubei/v1/music/get_queue"
    internal const val SEARCH = "/youtubei/v1/search"
    internal const val SEARCH_SUGGESTIONS = "/youtubei/v1/music/get_search_suggestions"

    internal const val MUSIC_RESPONSIVE_LIST_ITEM_RENDERER_MASK =
        "musicResponsiveListItemRenderer(flexColumns,fixedColumns,thumbnail,navigationEndpoint)"
    internal const val MUSIC_TWO_ROW_ITEM_RENDERER_MASK =
        "musicTwoRowItemRenderer(thumbnailRenderer,title,subtitle,navigationEndpoint)"
    const val PLAYLIST_PANEL_VIDEO_RENDERER_MASK =
        "playlistPanelVideoRenderer(title,navigationEndpoint,longBylineText,shortBylineText,thumbnail,lengthText)"

    internal fun HttpRequestBuilder.mask(value: String = "*") =
        header("X-Goog-FieldMask", value)

    @Serializable
    data class Info<T : NavigationEndpoint.Endpoint>(
        val name: String?,
        val endpoint: T?
    ) {
        @Suppress("UNCHECKED_CAST")
        constructor(run: Runs.Run) : this(
            name = cleanName(run.text),
            endpoint = run.navigationEndpoint?.endpoint as T?
        )

        companion object {
            fun cleanName(name: String?): String? {
                if (name == null || name.lowercase(Locale.ROOT) == "null" || name.isBlank()) return null
                
                // Remove "- Topic", " - Topic", "(Topic)", " Topic", etc. with various hyphens
                val cleaned = name
                    .replace(Regex("[\\s\\-–—]+Topic$", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("[\\s\\-–—]*\\(?Topic\\)?\\s*$", RegexOption.IGNORE_CASE), "")
                    .trim()
                
                return if (cleaned.lowercase(Locale.ROOT) == "topic" || cleaned.isEmpty()) null else cleaned
            }
        }
    }

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val Song = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val Video = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val Album = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val Artist = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val CommunityPlaylist = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FeaturedPlaylist = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
        }
    }

    @Serializable
    sealed class Item {
        abstract val thumbnail: Thumbnail?
        abstract val key: String
    }

    @Serializable
    data class SongItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val album: Info<NavigationEndpoint.Endpoint.Browse>?,
        val durationText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info?.endpoint?.videoId ?: ""

        companion object
    }

    @Serializable
    data class VideoItem(
        val info: Info<NavigationEndpoint.Endpoint.Watch>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val viewsText: String?,
        val durationText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info?.endpoint?.videoId ?: ""

        val isOfficialMusicVideo: Boolean
            get() = info
                ?.endpoint
                ?.watchEndpointMusicSupportedConfigs
                ?.watchEndpointMusicConfig
                ?.musicVideoType == "MUSIC_VIDEO_TYPE_OMV"

        companion object
    }

    @Serializable
    data class AlbumItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info?.endpoint?.browseId ?: ""

        companion object
    }

    @Serializable
    data class ArtistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val subscribersCountText: String?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info?.endpoint?.browseId ?: ""

        companion object
    }

    @Serializable
    data class PlaylistItem(
        val info: Info<NavigationEndpoint.Endpoint.Browse>?,
        val channel: Info<NavigationEndpoint.Endpoint.Browse>?,
        val songCount: Int?,
        override val thumbnail: Thumbnail?
    ) : Item() {
        override val key get() = info?.endpoint?.browseId ?: ""

        companion object
    }

    @Serializable
    data class ArtistPage(
        val name: String?,
        val description: String?,
        val thumbnail: Thumbnail?,
        val shuffleEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val radioEndpoint: NavigationEndpoint.Endpoint.Watch?,
        val songs: List<SongItem>?,
        val songsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val albums: List<AlbumItem>?,
        val albumsEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val singles: List<AlbumItem>?,
        val singlesEndpoint: NavigationEndpoint.Endpoint.Browse?,
        val playlists: List<PlaylistItem>?,
        val featuredPlaylists: List<PlaylistItem>?,
        val relatedArtists: List<ArtistItem>?
    )

    @Serializable
    data class PlaylistOrAlbumPage(
        val title: String?,
        val authors: List<Info<NavigationEndpoint.Endpoint.Browse>>?,
        val year: String?,
        val thumbnail: Thumbnail?,
        val url: String?,
        val songsPage: ItemsPage<SongItem>?,
        val otherVersions: List<AlbumItem>?,
        val relatedAlbums: List<AlbumItem>?
    )

    @Serializable
    data class NextPage(
        val itemsPage: ItemsPage<SongItem>?,
        val playlistId: String?,
        val params: String? = null,
        val playlistSetVideoId: String? = null
    )

    @Serializable
    data class RelatedPage(
        val songs: List<SongItem>? = null,
        val playlists: List<PlaylistItem>? = null,
        val albums: List<AlbumItem>? = null,
        val artists: List<ArtistItem>? = null,
    )

    @Serializable
    data class ItemsPage<T : Item>(
        val items: List<T>?,
        val continuation: String?
    )
}
