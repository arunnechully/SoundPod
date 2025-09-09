package com.github.innertube.models.bodies

import com.github.innertube.models.Context
import com.github.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable

@Serializable
data class SearchSuggestionsBody(
    val context: Context = YouTubeClient.WEB_REMIX.toContext(),
    val input: String
)
