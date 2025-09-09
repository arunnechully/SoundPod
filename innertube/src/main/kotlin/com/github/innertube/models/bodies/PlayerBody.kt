package com.github.innertube.models.bodies

import com.github.innertube.models.Context
import com.github.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context = YouTubeClient.IOS.toContext(),
    val videoId: String,
    val playlistId: String? = null
)