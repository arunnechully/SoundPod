package com.github.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ButtonRenderer(
    val navigationEndpoint: NavigationEndpoint?
)
