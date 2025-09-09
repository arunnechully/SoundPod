package com.github.innertube.models.bodies

import com.github.innertube.models.Context
import com.github.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class QueueBody(
    val context: Context = YouTubeClient.WEB_REMIX.toContext(),
    val videoIds: List<String>? = null,
    val playlistId: String? = null,
)
