package com.github.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Release(
    val draft: Boolean,
    val name: String,
    val prerelease: Boolean,
    val assets: List<Asset> = emptyList()
)

@Serializable
data class Asset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String
)

fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024
    val mb = kb / 1024
    return when {
        mb > 0 -> "$mb MB"
        kb > 0 -> "$kb KB"
        else -> "$bytes bytes"
    }
}
