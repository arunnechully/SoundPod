package com.github.soundpod

import com.github.innertube.Innertube
import com.github.innertube.models.PlayerResponse
import com.github.innertube.requests.player
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class NewPipeDownloader private constructor() : Downloader() {
    private val client = OkHttpClient.Builder()
        .cache(Cache(File(MainApplication.appContext.cacheDir, "newpipe_cache"), 10 * 1024 * 1024))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val jsCacheFile = File(MainApplication.appContext.cacheDir, "base_js_content")
    private val jsUrlFile = File(MainApplication.appContext.cacheDir, "base_js_url")
    private val playerResponseCache = ConcurrentHashMap<String, Pair<PlayerResponse, Long>>()

    override fun execute(request: Request): Response {
        val url = request.url()
        val method = request.httpMethod()

        if (method == "GET" && url.contains("base.js")) {
            if (jsUrlFile.exists() && jsUrlFile.readText() == url && jsCacheFile.exists()) {
                val lastModified = jsCacheFile.lastModified()
                if (System.currentTimeMillis() - lastModified < TimeUnit.DAYS.toMillis(1)) {
                    return Response(200, "OK", mapOf("Content-Type" to listOf("application/javascript")), jsCacheFile.readText(), url)
                }
            }
        }

        if (method == "GET" && (url.contains("youtube.com/watch?v=") || url.contains("music.youtube.com/watch?v="))) {
            val videoId = url.substringAfter("v=").substringBefore("&").substringBefore("?")
            if (videoId.length == 11) {
                val currentTime = System.currentTimeMillis()
                val cached = playerResponseCache[videoId]
                
                if (cached != null && (currentTime - cached.second) < TimeUnit.MINUTES.toMillis(5)) {
                    val playerResponseJson = json.encodeToString(cached.first)
                    val html = "<html><head><script>var ytInitialPlayerResponse = $playerResponseJson;</script></head><body></body></html>"
                    return Response(200, "OK", mapOf("Content-Type" to listOf("text/html")), html, url)
                }

                try {
                    val playerResponse = runBlocking { Innertube.player(videoId) }?.getOrNull()?.also {
                        playerResponseCache[videoId] = it to currentTime
                    }

                    if (playerResponse != null) {
                        val playerResponseJson = json.encodeToString(playerResponse)
                        val html = "<html><head><script>var ytInitialPlayerResponse = $playerResponseJson;</script></head><body></body></html>"
                        return Response(200, "OK", mapOf("Content-Type" to listOf("text/html")), html, url)
                    }
                } catch (e: Exception) {
                }
            }
        }

        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val builder = okhttp3.Request.Builder()
            .url(url)

        headers.forEach { (name, values) ->
            values.forEach { value ->
                builder.addHeader(name, value)
            }
        }

        val requestBody = dataToSend?.toRequestBody()
            ?: if (method == "POST") ByteArray(0).toRequestBody() else null

        builder.method(method, requestBody)

        val response = client.newCall(builder.build()).execute()
        val body = response.body?.string()

        if (method == "GET" && url.contains("base.js") && response.isSuccessful && body != null) {
            jsUrlFile.writeText(url)
            jsCacheFile.writeText(body)
        }

        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            body,
            response.request.url.toString(),
        )
    }

    companion object {
        private var instance: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader {
            if (instance == null) {
                instance = NewPipeDownloader()
            }
            return instance!!
        }
    }
}
