package com.github.soundpod.service

import android.util.Log
import com.github.innertube.Innertube
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YouTubeBootstrap {
    private const val TAG = "SoundPod-Bootstrap"
    private const val MUSIC_URL = "https://music.youtube.com/"
    
    private val client = HttpClient(OkHttp) {
        followRedirects = true
    }

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting bootstrap from $MUSIC_URL")
                
                val response = client.get(MUSIC_URL) {
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                    header("Accept-Language", "en-US,en;q=0.9")
                }
                val html = response.bodyAsText()

                val apiKey = extractValue(html, "INNERTUBE_API_KEY")
                val clientVersion = extractValue(html, "INNERTUBE_CLIENT_VERSION")
                val visitorData = extractValue(html, "VISITOR_DATA")
                val jsUrl = extractJsUrl(html)

                Log.d(TAG, "Extracted: apiKey=$apiKey, version=$clientVersion, visitorData=$visitorData, jsUrl=$jsUrl")

                if (apiKey != null || clientVersion != null || visitorData != null || jsUrl != null) {
                    YouTubeSessionManager.updateSession(
                        apiKey = apiKey,
                        clientVersion = clientVersion,
                        visitorData = visitorData,
                        jsUrl = jsUrl
                    )
                    Log.i(TAG, "Bootstrap successful: Applied dynamic session values")
                } else {
                    Log.w(TAG, "Bootstrap failed: Could not extract any values from HTML")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bootstrap failed during initialization", e)
            }
        }
    }

    private fun extractValue(html: String, key: String): String? {
        // Matches "key":"value" or "key": "value"
        val regex = Regex(""""$key"\s*:\s*"([^"]+)"""")
        return regex.find(html)?.groupValues?.get(1)
    }

    private fun extractJsUrl(html: String): String? {
        // Look for the main player JS file
        val regex = Regex("""/s/player/[a-zA-Z0-9]+/player_ias\.vflset/[a-zA-Z0-9_/.]+/base\.js""")
        return regex.find(html)?.value
    }
}
