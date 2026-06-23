package com.github.soundpod.extractor

import com.github.innertube.Innertube
import com.github.innertube.models.PlayerResponse
import com.github.innertube.requests.player
import kotlinx.coroutines.runBlocking
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

class NewPipeDownloader private constructor(cacheDir: File) : Downloader() {
    private val client = OkHttpClient.Builder()
        .cache(Cache(File(cacheDir, "newpipe_cache"), 10 * 1024 * 1024))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val jsCacheFile = File(cacheDir, "base_js_content")
    private val jsUrlFile = File(cacheDir, "base_js_url")
    private val playerResponseCache = ConcurrentHashMap<String, Pair<PlayerResponse, Long>>()

    fun preCache(videoId: String, playerResponse: PlayerResponse) {
        playerResponseCache[videoId] = playerResponse to System.currentTimeMillis()
    }

    override fun execute(request: Request): Response {
        val url = request.url()
        val method = request.httpMethod()

        // Cache base.js to speed up extraction
        if (method == "GET" && url.contains("base.js")) {
            if (jsUrlFile.exists() && jsUrlFile.readText() == url && jsCacheFile.exists()) {
                val lastModified = jsCacheFile.lastModified()
                if (System.currentTimeMillis() - lastModified < TimeUnit.DAYS.toMillis(1)) {
                    return Response(200, "OK", mapOf("Content-Type" to listOf("application/javascript")), jsCacheFile.readText(), url)
                }
            }
        }

        // Intercept YouTube watch requests to inject InnerTube data
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
                        playerResponseCache[videoId] = it.response to currentTime
                    }

                    if (playerResponse != null) {
                        val playerResponseJson = json.encodeToString(playerResponse.response)
                        val html = "<html><head><script>var ytInitialPlayerResponse = $playerResponseJson;</script></head><body></body></html>"
                        return Response(200, "OK", mapOf("Content-Type" to listOf("text/html")), html, url)
                    }
                } catch (_: Exception) {
                }
            }
        }

        val headers = request.headers().toMutableMap()
        val dataToSend = request.dataToSend()

        val builder = okhttp3.Request.Builder()
            .url(url)

        headers.forEach { (name, values) ->
            values.forEach { value ->
                builder.addHeader(name, value)
            }
        }
        
        // Ensure Cookies are synced
        if (headers.none { it.key.equals("Cookie", ignoreCase = true) }) {
            Innertube.cookies?.let { builder.addHeader("Cookie", it) }
        }
        
        if (headers.none { it.key.equals("User-Agent", ignoreCase = true) }) {
            builder.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
        }

        val requestBody = dataToSend?.toRequestBody()
            ?: if (method == "POST") ByteArray(0).toRequestBody() else null

        builder.method(method, requestBody)

        val response = client.newCall(builder.build()).execute()
        val body = response.body.string()

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

        fun init(cacheDir: File) {
            if (instance == null) {
                instance = NewPipeDownloader(cacheDir)
            }
        }

        fun getInstance(): NewPipeDownloader {
            return instance ?: throw IllegalStateException("NewPipeDownloader must be initialized with init(cacheDir)")
        }
    }
}
