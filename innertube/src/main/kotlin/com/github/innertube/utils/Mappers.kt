package com.github.innertube.utils

import io.ktor.utils.io.CancellationException
import com.github.innertube.Innertube
import com.github.innertube.models.*

// --- Utils.kt ---

internal fun SectionListRenderer.findSectionByTitle(vararg titles: String): SectionListRenderer.Content? {
    return contents?.find { content ->
        val title = content
            .musicCarouselShelfRenderer
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.title
            ?: content
                .musicShelfRenderer
                ?.title

        val text = title?.runs?.firstOrNull()?.text ?: return@find false
        titles.any { text.contains(it, ignoreCase = true) }
    }
}

internal fun SectionListRenderer.findSectionByStrapline(text: String): SectionListRenderer.Content? {
    return contents?.find { content ->
        content
            .musicCarouselShelfRenderer
            ?.header
            ?.musicCarouselShelfBasicHeaderRenderer
            ?.strapline
            ?.runs
            ?.firstOrNull()
            ?.text == text
    }
}

internal inline fun <R> runCatchingNonCancellable(block: () -> R): Result<R>? {
    val result = runCatching(block)
    return when (result.exceptionOrNull()) {
        is CancellationException -> null
        else -> result
    }
}

infix operator fun <T : Innertube.Item> Innertube.ItemsPage<T>?.plus(other: Innertube.ItemsPage<T>) =
    other.copy(
        items = (this?.items?.plus(other.items ?: emptyList())
            ?: other.items)?.distinctBy(Innertube.Item::key)
    )

// --- FromMusicResponsiveListItemRenderer.kt ---

fun Innertube.SongItem.Companion.from(renderer: MusicResponsiveListItemRenderer): Innertube.SongItem? {
    return Innertube.SongItem(
        info = renderer
            .flexColumns
            .getOrNull(0)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.getOrNull(0)
            ?.let(Innertube::Info),
        authors = renderer
            .flexColumns
            .getOrNull(1)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.map<Runs.Run, Innertube.Info<NavigationEndpoint.Endpoint.Browse>>(Innertube::Info)
            ?.takeIf(List<Any>::isNotEmpty),
        durationText = renderer
            .fixedColumns
            ?.getOrNull(0)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.getOrNull(0)
            ?.text,
        album = renderer
            .flexColumns
            .getOrNull(2)
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.firstOrNull()
            ?.let(Innertube::Info),
        thumbnail = renderer
            .thumbnail
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.lastOrNull()
    ).takeIf { it.info?.endpoint?.videoId != null }
}

// --- FromMusicShelfRendererContent.kt ---

fun Innertube.SongItem.Companion.from(content: MusicShelfRenderer.Content): Innertube.SongItem? {
    val (mainRuns, otherRuns) = content.runs

    // Possible configurations:
    // "song" • author(s) • album • duration
    // "song" • author(s) • duration
    // author(s) • album • duration
    // author(s) • duration

    val album: Innertube.Info<NavigationEndpoint.Endpoint.Browse>? = otherRuns
        .getOrNull(otherRuns.lastIndex - 1)
        ?.firstOrNull()
        ?.takeIf { run ->
            run
                .navigationEndpoint
                ?.browseEndpoint
                ?.type == "MUSIC_PAGE_TYPE_ALBUM"
        }
        ?.let(Innertube::Info)

    return Innertube.SongItem(
        info = mainRuns
            .firstOrNull()
            ?.let(Innertube::Info),
        authors = otherRuns
            .getOrNull(otherRuns.lastIndex - if (album == null) 1 else 2)
            ?.map(Innertube::Info),
        album = album,
        durationText = otherRuns
            .lastOrNull()
            ?.firstOrNull()?.text,
        thumbnail = content
            .thumbnail
    ).takeIf { it.info?.endpoint?.videoId != null }
}

fun Innertube.VideoItem.Companion.from(content: MusicShelfRenderer.Content): Innertube.VideoItem? {
    val (mainRuns, otherRuns) = content.runs

    return Innertube.VideoItem(
        info = mainRuns
            .firstOrNull()
            ?.let(Innertube::Info),
        authors = otherRuns
            .getOrNull(otherRuns.lastIndex - 2)
            ?.map(Innertube::Info),
        viewsText = otherRuns
            .getOrNull(otherRuns.lastIndex - 1)
            ?.firstOrNull()
            ?.text,
        durationText = otherRuns
            .getOrNull(otherRuns.lastIndex)
            ?.firstOrNull()
            ?.text,
        thumbnail = content
            .thumbnail
    ).takeIf { it.info?.endpoint?.videoId != null }
}

fun Innertube.AlbumItem.Companion.from(content: MusicShelfRenderer.Content): Innertube.AlbumItem? {
    val (mainRuns, otherRuns) = content.runs

    return Innertube.AlbumItem(
        info = Innertube.Info(
            name = Innertube.Info.cleanName(mainRuns
                .firstOrNull()
                ?.text),
            endpoint = content
                .musicResponsiveListItemRenderer
                ?.navigationEndpoint
                ?.browseEndpoint
        ),
        authors = otherRuns
            .getOrNull(otherRuns.lastIndex - 1)
            ?.map(Innertube::Info),
        year = otherRuns
            .getOrNull(otherRuns.lastIndex)
            ?.firstOrNull()
            ?.text,
        thumbnail = content
            .thumbnail
    ).takeIf { it.info?.endpoint?.browseId != null }
}

fun Innertube.ArtistItem.Companion.from(content: MusicShelfRenderer.Content): Innertube.ArtistItem? {
    val (mainRuns, otherRuns) = content.runs

    return Innertube.ArtistItem(
        info = Innertube.Info(
            name = Innertube.Info.cleanName(mainRuns
                .firstOrNull()
                ?.text),
            endpoint = content
                .musicResponsiveListItemRenderer
                ?.navigationEndpoint
                ?.browseEndpoint
        ),
        subscribersCountText = otherRuns
            .lastOrNull()
            ?.last()
            ?.text,
        thumbnail = content
            .thumbnail
    ).takeIf { it.info?.endpoint?.browseId != null }
}

fun Innertube.PlaylistItem.Companion.from(content: MusicShelfRenderer.Content): Innertube.PlaylistItem? {
    val (mainRuns, otherRuns) = content.runs

    return Innertube.PlaylistItem(
        info = Innertube.Info(
            name = Innertube.Info.cleanName(mainRuns
                .firstOrNull()
                ?.text),
            endpoint = content
                .musicResponsiveListItemRenderer
                ?.navigationEndpoint
                ?.browseEndpoint
        ),
        channel = otherRuns
            .firstOrNull()
            ?.firstOrNull()
            ?.let(Innertube::Info),
        songCount = otherRuns
            .lastOrNull()
            ?.firstOrNull()
            ?.text
            ?.split(' ')
            ?.firstOrNull()
            ?.toIntOrNull(),
        thumbnail = content
            .thumbnail
    ).takeIf { it.info?.endpoint?.browseId != null }
}

// --- FromMusicTwoRowItemRenderer.kt ---

fun Innertube.AlbumItem.Companion.from(renderer: MusicTwoRowItemRenderer): Innertube.AlbumItem? {
    return Innertube.AlbumItem(
        info = renderer
            .title
            ?.runs
            ?.firstOrNull()
            ?.let(Innertube::Info),
        authors = null,
        year = renderer
            .subtitle
            ?.runs
            ?.lastOrNull()
            ?.text,
        thumbnail = renderer
            .thumbnailRenderer
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.lastOrNull()
    ).takeIf { it.info?.endpoint?.browseId != null }
}

fun Innertube.ArtistItem.Companion.from(renderer: MusicTwoRowItemRenderer): Innertube.ArtistItem? {
    return Innertube.ArtistItem(
        info = renderer
            .title
            ?.runs
            ?.firstOrNull()
            ?.let(Innertube::Info),
        subscribersCountText = renderer
            .subtitle
            ?.runs
            ?.firstOrNull()
            ?.text,
        thumbnail = renderer
            .thumbnailRenderer
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.lastOrNull()
    ).takeIf { it.info?.endpoint?.browseId != null }
}

fun Innertube.PlaylistItem.Companion.from(renderer: MusicTwoRowItemRenderer): Innertube.PlaylistItem? {
    return Innertube.PlaylistItem(
        info = renderer
            .title
            ?.runs
            ?.firstOrNull()
            ?.let(Innertube::Info),
        channel = renderer
            .subtitle
            ?.runs
            ?.getOrNull(2)
            ?.let(Innertube::Info),
        songCount = renderer
            .subtitle
            ?.runs
            ?.getOrNull(4)
            ?.text
            ?.split(' ')
            ?.firstOrNull()
            ?.toIntOrNull(),
        thumbnail = renderer
            .thumbnailRenderer
            ?.musicThumbnailRenderer
            ?.thumbnail
            ?.thumbnails
            ?.lastOrNull()
    ).takeIf { it.info?.endpoint?.browseId != null }
}

// --- FromPlaylistPanelVideoRenderer.kt ---

fun Innertube.SongItem.Companion.from(renderer: PlaylistPanelVideoRenderer): Innertube.SongItem? {
    return Innertube.SongItem(
        info = Innertube.Info(
            name = Innertube.Info.cleanName(renderer
                .title
                ?.text),
            endpoint = renderer
                .navigationEndpoint
                ?.watchEndpoint
        ),
        authors = renderer
            .longBylineText
            ?.splitBySeparator()
            ?.getOrNull(0)
            ?.map(Innertube::Info),
        album = renderer
            .longBylineText
            ?.splitBySeparator()
            ?.getOrNull(1)
            ?.getOrNull(0)
            ?.let(Innertube::Info),
        thumbnail = renderer
            .thumbnail
            ?.thumbnails
            ?.lastOrNull(),
        durationText = renderer
            .lengthText
            ?.text
    ).takeIf { it.info?.endpoint?.videoId != null }
}
