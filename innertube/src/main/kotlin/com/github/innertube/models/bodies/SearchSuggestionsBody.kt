package com.github.innertube.models.bodies

import com.github.innertube.models.Context
import com.github.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsBody(
    val context: Context = YouTubeClient.MAC_SAFARI_WEB_REMIX.toContext(),
    val input: String
)
