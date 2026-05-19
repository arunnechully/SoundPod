package com.github.innertube.models.bodies

import com.github.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context,
    val videoId: String,
    val playlistId: String? = null,
    val cpn: String? = null,
    val playbackContext: PlaybackContext? = null
)
@Serializable
data class PlaybackContext(
    val contentPlaybackContext: ContentPlaybackContext
)

@Serializable
data class ContentPlaybackContext(
    val signatureTimestamp: Int
)