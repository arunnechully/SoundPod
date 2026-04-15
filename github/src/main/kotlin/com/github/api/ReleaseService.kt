package com.github.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header

class ReleaseService(
    private val client: HttpClient
) {
    suspend fun getReleases(): List<Release> {
        return try {
            val response =
                client.get("https://api.github.com/repos/arunnechully/SoundPod/releases") {
                    header("X-GitHub-Api-Version", "2022-11-28")
                }
            response.body()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getLatestReleaseApkUrl(flavorName: String): String? {
        val releases = getReleases()

        val latestRelease = releases.firstOrNull { !it.draft && !it.prerelease } ?: return null

        val matchedAsset = latestRelease.assets.find { asset ->
            asset.name.contains(flavorName, ignoreCase = true) && asset.name.endsWith(".apk")
        }

        return matchedAsset?.browserDownloadUrl
    }
}