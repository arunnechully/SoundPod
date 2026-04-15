package com.github.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class Release(
    @SerialName("tag_name")
    val tagName: String,
    val draft: Boolean,
    val name: String? = null,
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
    val kb = bytes / 1024.0
    val mb = kb / 1024.0

    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
        else -> "$bytes bytes"
    }
}