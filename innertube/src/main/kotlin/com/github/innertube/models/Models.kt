@file:OptIn(ExperimentalSerializationApi::class)

package com.github.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class BrowseResponse(
    val contents: Contents?,
    val header: Header?,
    val microformat: Microformat?
) {
    @Serializable
    data class Contents(
        val singleColumnBrowseResultsRenderer: Tabs?,
        val sectionListRenderer: SectionListRenderer?,
        val twoColumnBrowseResultsRenderer: TwoColumnBrowseResultsRenderer?
    )

    @Serializable
    data class Header(
        @JsonNames("musicVisualHeaderRenderer")
        val musicImmersiveHeaderRenderer: MusicImmersiveHeaderRenderer?,
        val musicDetailHeaderRenderer: MusicDetailHeaderRenderer?
    ) {
        @Serializable
        data class MusicDetailHeaderRenderer(
            val title: Runs?,
            val subtitle: Runs?,
            val secondSubtitle: Runs?,
            val thumbnail: ThumbnailRenderer?
        )

        @Serializable
        data class MusicImmersiveHeaderRenderer(
            val description: Runs?,
            val playButton: PlayButton?,
            val startRadioButton: StartRadioButton?,
            val thumbnail: ThumbnailRenderer?,
            val foregroundThumbnail: ThumbnailRenderer?,
            val title: Runs?
        ) {
            @Serializable
            data class PlayButton(
                val buttonRenderer: ButtonRenderer?
            )

            @Serializable
            data class StartRadioButton(
                val buttonRenderer: ButtonRenderer?
            )
        }
    }

    @Serializable
    data class Microformat(
        val microformatDataRenderer: MicroformatDataRenderer?
    ) {
        @Serializable
        data class MicroformatDataRenderer(
            val urlCanonical: String?
        )
    }
}

@Serializable
data class ButtonRenderer(
    val navigationEndpoint: NavigationEndpoint?
)

@Serializable
data class Continuation(
    @JsonNames("nextContinuationData", "nextRadioContinuationData")
    val nextContinuationData: Data?
) {
    @Serializable
    data class Data(
        val continuation: String?
    )
}

@Serializable
data class ContinuationResponse(
    val continuationContents: ContinuationContents?,
) {
    @Serializable
    data class ContinuationContents(
        @JsonNames("musicPlaylistShelfContinuation", "musicShelfContinuation")
        val musicShelfContinuation: MusicShelfRenderer?,
        val sectionListContinuation: SectionListRenderer?,
        val playlistPanelContinuation: NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer?,
    )
}

@Serializable
data class GetQueueResponse(
    val queueDatas: List<QueueData>?,
) {
    @Serializable
    data class QueueData(
        val content: NextResponse.MusicQueueRenderer.Content.PlaylistPanelRenderer.Content?
    )
}

@Serializable
data class GridRenderer(
    val items: List<Item>?,
) {
    @Serializable
    data class Item(
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?
    )
}

@Serializable
data class MusicCarouselShelfRenderer(
    val header: Header?,
    val contents: List<Content>?,
) {
    @Serializable
    data class Content(
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?,
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
    )

    @Serializable
    data class Header(
        val musicTwoRowItemRenderer: MusicTwoRowItemRenderer?,
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
        val musicCarouselShelfBasicHeaderRenderer: MusicCarouselShelfBasicHeaderRenderer?
    ) {
        @Serializable
        data class MusicCarouselShelfBasicHeaderRenderer(
            val moreContentButton: MoreContentButton?,
            val title: Runs?,
            val strapline: Runs?,
        ) {
            @Serializable
            data class MoreContentButton(
                val buttonRenderer: ButtonRenderer?
            )
        }
    }
}

@Serializable
data class MusicPlaylistShelfRenderer(
    val contents: List<Content>?,
    val continuations: List<Continuation>? = null,
    val playlistId: String? = null,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null
    )
}

@Serializable
data class MusicResponsiveListItemRenderer(
    val fixedColumns: List<FlexColumn>?,
    val flexColumns: List<FlexColumn>,
    val thumbnail: ThumbnailRenderer?,
    val navigationEndpoint: NavigationEndpoint?,
) {
    @Serializable
    data class FlexColumn(
        @JsonNames("musicResponsiveListItemFixedColumnRenderer")
        val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer?
    ) {
        @Serializable
        data class MusicResponsiveListItemFlexColumnRenderer(
            val text: Runs?
        )
    }
}

@Serializable
data class MusicShelfRenderer(
    val bottomEndpoint: NavigationEndpoint?,
    val contents: List<Content>?,
    val continuations: List<Continuation>?,
    val title: Runs?
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
    ) {
        val runs: Pair<List<Runs.Run>, List<List<Runs.Run>>>
            get() = (musicResponsiveListItemRenderer
                ?.flexColumns
                ?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?: emptyList()) to
                    (musicResponsiveListItemRenderer
                        ?.flexColumns
                        ?.getOrNull(1)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.splitBySeparator()
                        ?: emptyList()
                            )

        val thumbnail: Thumbnail?
            get() = musicResponsiveListItemRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.thumbnail
                ?.thumbnails
                ?.lastOrNull()
    }
}

@Serializable
data class MusicTwoRowItemRenderer(
    val navigationEndpoint: NavigationEndpoint?,
    val thumbnailRenderer: ThumbnailRenderer?,
    val title: Runs?,
    val subtitle: Runs?,
)

@Serializable
data class NextResponse(
    val contents: Contents?
) {
    @Serializable
    data class MusicQueueRenderer(
        val content: Content?
    ) {
        @Serializable
        data class Content(
            @JsonNames("playlistPanelContinuation")
            val playlistPanelRenderer: PlaylistPanelRenderer?
        ) {
            @Serializable
            data class PlaylistPanelRenderer(
                val contents: List<Content>?,
                val continuations: List<Continuation>?,
            ) {
                @Serializable
                data class Content(
                    val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer?,
                    val automixPreviewVideoRenderer: AutomixPreviewVideoRenderer?,
                ) {

                    @Serializable
                    data class AutomixPreviewVideoRenderer(
                        val content: Content?
                    ) {
                        @Serializable
                        data class Content(
                            val automixPlaylistVideoRenderer: AutomixPlaylistVideoRenderer?
                        ) {
                            @Serializable
                            data class AutomixPlaylistVideoRenderer(
                                val navigationEndpoint: NavigationEndpoint?
                            )
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer?
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer?
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tab>?
                ) {
                    @Serializable
                    data class Tab(
                        val tabRenderer: TabRenderer?
                    ) {
                        @Serializable
                        data class TabRenderer(
                            val content: Content?,
                            val endpoint: NavigationEndpoint?,
                            val title: String?
                        ) {
                            @Serializable
                            data class Content(
                                val musicQueueRenderer: MusicQueueRenderer?
                            )
                        }
                    }
                }
            }
        }
    }
}

@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus?,
    val playerConfig: PlayerConfig?,
    val streamingData: StreamingData?,
    val videoDetails: VideoDetails?,
) {
    @Serializable
    data class PlayabilityStatus(
        val status: String?,
        val reason: String? = null,
        val messages: List<String>? = null
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig?
    ) {
        @Serializable
        data class AudioConfig(
            private val loudnessDb: Double?,
            private val perceptualLoudnessDb: Double?
        ) {
            // For music clients only
            val normalizedLoudnessDb: Float?
                get() = (loudnessDb ?: perceptualLoudnessDb)?.plus(7)?.toFloat()
        }
    }

    @Serializable
    data class StreamingData(
        val adaptiveFormats: List<AdaptiveFormat>?,
        val formats: List<AdaptiveFormat>? = null
    ) {
        val highestQualityFormat: AdaptiveFormat?
            get() {
                val combined = adaptiveFormats.orEmpty() + formats.orEmpty()
                val audioFormats = combined.filter { (it.url != null || it.signatureCipher != null) && it.mimeType.startsWith("audio/") }
                
                if (audioFormats.isNotEmpty()) {
                    // Strictly prioritize Opus (itag 251, 250, 249) over AAC (itag 140, 139)
                    return audioFormats.find { it.itag == 251 } // Opus 160kbps
                        ?: audioFormats.find { it.itag == 250 } // Opus 64kbps
                        ?: audioFormats.find { it.itag == 249 } // Opus 48kbps
                        ?: audioFormats.find { it.itag == 140 } // AAC 128kbps
                        ?: audioFormats.find { it.itag == 139 } // AAC 48kbps
                        ?: audioFormats.maxByOrNull { it.bitrate ?: 0L }
                }

                return combined.find { (it.url != null || it.signatureCipher != null) && it.mimeType.startsWith("video/") }
            }

        @Serializable
        data class AdaptiveFormat(
            val itag: Int,
            val mimeType: String,
            val bitrate: Long?,
            val averageBitrate: Long?,
            val contentLength: Long?,
            val audioQuality: String?,
            val approxDurationMs: Long?,
            val lastModified: Long?,
            val loudnessDb: Double?,
            val audioSampleRate: Int?,
            val url: String? = null,
            val signatureCipher: String? = null,
        )
    }

    @Serializable
    data class VideoDetails(
        val videoId: String?
    )
}

@Serializable
data class PlaylistPanelVideoRenderer(
    val title: Runs?,
    val longBylineText: Runs?,
    val shortBylineText: Runs?,
    val lengthText: Runs?,
    val navigationEndpoint: NavigationEndpoint?,
    val thumbnail: ThumbnailRenderer.MusicThumbnailRenderer.Thumbnail?,
)

@Serializable
data class Runs(
    val runs: List<Run> = listOf()
) {
    val text: String
        get() = runs.joinToString("") { it.text ?: "" }

    fun splitBySeparator(): List<List<Run>> {
        return runs.flatMapIndexed { index, run ->
            when {
                index == 0 || index == runs.lastIndex -> listOf(index)
                run.text == " • " -> listOf(index - 1, index + 1)
                else -> emptyList()
            }
        }.windowed(size = 2, step = 2) { (from, to) -> runs.slice(from..to) }.let {
            it.ifEmpty {
                listOf(runs)
            }
        }
    }

    @Serializable
    data class Run(
        val text: String?,
        val navigationEndpoint: NavigationEndpoint?,
    )
}

@Serializable
data class SearchResponse(
    val contents: Contents?,
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs?
    )
}

@Serializable
data class SearchSuggestionsResponse(
    val contents: List<Content>?
) {
    @Serializable
    data class Content(
        val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer?
    ) {
        @Serializable
        data class SearchSuggestionsSectionRenderer(
            val contents: List<Content>?
        ) {
            @Serializable
            data class Content(
                val searchSuggestionRenderer: SearchSuggestionRenderer?
            ) {
                @Serializable
                data class SearchSuggestionRenderer(
                    val navigationEndpoint: NavigationEndpoint?,
                )
            }
        }
    }
}

@Serializable
data class SectionListRenderer(
    val contents: List<Content>?,
    val continuations: List<Continuation>?
) {
    @Serializable
    data class Content(
        @JsonNames("musicImmersiveCarouselShelfRenderer")
        val musicCarouselShelfRenderer: MusicCarouselShelfRenderer?,
        val musicShelfRenderer: MusicShelfRenderer?,
        val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer?,
        val gridRenderer: GridRenderer?,
        val musicDescriptionShelfRenderer: MusicDescriptionShelfRenderer?,
        val musicResponsiveHeaderRenderer: MusicResponsiveHeaderRenderer?
    ){
        @Serializable
        data class MusicDescriptionShelfRenderer(
            val description: Runs?
        )

        @Serializable
        data class MusicResponsiveHeaderRenderer(
            val title: Runs?,
            val subtitle: Runs?,
            val secondSubtitle: Runs?,
            val thumbnail: ThumbnailRenderer?,
            val straplineTextOne: Runs?
        )
    }
}

@Serializable
data class Tabs(
    val tabs: List<Tab>?
) {
    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer?
    ) {
        @Serializable
        data class TabRenderer(
            val content: Content?,
            val title: String?,
            val tabIdentifier: String?
        ) {
            @Serializable
            data class Content(
                val sectionListRenderer: SectionListRenderer?
            )
        }
    }
}

@Serializable
data class TwoColumnBrowseResultsRenderer(
    val tabs: List<Tabs.Tab>?,
    val secondaryContents: Tabs.Tab.TabRenderer.Content?
)

@Serializable
data class Thumbnail(
    val url: String,
    val height: Int?,
    val width: Int?
) {
    val isResizable: Boolean
        get() = !url.startsWith("https://i.ytimg.com")

    fun size(size: Int): String {
        return when {
            url.startsWith("https://lh3.googleusercontent.com") ||
            url.startsWith("https://yt3.ggpht.com") -> {
                val cleanUrl = url.substringBefore("=")
                "$cleanUrl=w$size-h$size-p-l100-rj"
            }
            else -> url
        }
    }
}

@Serializable
data class ThumbnailRenderer(
    @JsonNames("croppedSquareThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer?
) {
    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: Thumbnail?
    ) {
        @Serializable
        data class Thumbnail(
            val thumbnails: List<com.github.innertube.models.Thumbnail>?
        )
    }
}
