package com.github.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object GitHub {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json { ignoreUnknownKeys = true }
            )
        }
    }

    suspend fun getLastestRelease(): Release? {
        val response = ReleaseService(client = client).getReleases()
        val filteredReleases = response.filter { !it.draft && !it.prerelease }
        return filteredReleases.firstOrNull()
    }
    suspend fun getLatestReleaseApkUrl(flavorName: String): String? {
        return ReleaseService(client = client).getLatestReleaseApkUrl(flavorName)
    }
}