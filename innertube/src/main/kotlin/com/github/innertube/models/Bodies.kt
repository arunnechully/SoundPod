package com.github.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val localized: Boolean = true,
    val context: Context = YouTubeClient.WEB_REMIX.toContext(localized = localized),
    val browseId: String,
    val params: String? = null
)

@Serializable
data class ContinuationBody(
    val context: Context = YouTubeClient.WEB_REMIX.toContext(),
    val continuation: String,
)

@Serializable
data class NextBody(
    val context: Context = YouTubeClient.WEB_REMIX.toContext(),
    val videoId: String?,
    val isAudioOnly: Boolean = true,
    val playlistId: String? = null,
    val tunerSettingValue: String = "AUTOMIX_SETTING_NORMAL",
    val index: Int? = null,
    val params: String? = null,
    val playlistSetVideoId: String? = null,
    val watchEndpointMusicSupportedConfigs: WatchEndpointMusicSupportedConfigs = WatchEndpointMusicSupportedConfigs(
        musicVideoType = "MUSIC_VIDEO_TYPE_ATV"
    )
) {
    @Serializable
    data class WatchEndpointMusicSupportedConfigs(
        val musicVideoType: String
    )
}

@Serializable
data class PlayerBody(
    val context: Context,
    val videoId: String,
    val playlistId: String? = null,
    val serviceIntegrityDimensions: ServiceIntegrityDimensions? = null
)

@Serializable
data class QueueBody(
    val context: Context = YouTubeClient.WEB_REMIX.toContext(),
    val videoIds: List<String>? = null,
    val playlistId: String? = null,
)

@Serializable
data class SearchBody(
    val context: Context = YouTubeClient.WEB_REMIX.toContext(),
    val query: String,
    val params: String
)

@Serializable
data class SearchSuggestionsBody(
    val context: Context = YouTubeClient.WEB_REMIX.toContext(),
    val input: String
)

@Serializable
data class ServiceIntegrityDimensions(
    val poToken: String
)
