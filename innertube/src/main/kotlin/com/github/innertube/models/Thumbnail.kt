package com.github.innertube.models

import kotlinx.serialization.Serializable

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
                "$cleanUrl=w1024-h1024-p-l100-rj"
            }
            else -> url
        }
    }
}
