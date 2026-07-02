package com.github.soundpod

import android.util.Log
import com.github.innertube.Innertube
import com.github.innertube.models.PlayerResponse
import com.github.innertube.requests.player
import com.github.soundpod.service.YouTubeSessionManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class NewPipeDownloader private constructor() : Downloader() {
    private val client = Innertube.okHttpClient.newBuilder()
        .cache(Cache(File(MainApplication.appContext.cacheDir, "newpipe_cache"), 10 * 1024 * 1024))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val jsCacheFile = File(MainApplication.appContext.cacheDir, "base_js_content")
    private val jsUrlFile = File(MainApplication.appContext.cacheDir, "base_js_url")
    private val playerResponseCache = ConcurrentHashMap<String, Pair<PlayerResponse, Long>>()

    fun preCache(videoId: String, playerResponse: PlayerResponse) {
        playerResponseCache[videoId] = playerResponse to System.currentTimeMillis()
    }

    @Throws(IOException::class)
    fun downloadFile(url: String, targetFile: File, progressListener: ((Long, Long) -> Unit)? = null) {
        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("User-Agent", Innertube.USER_AGENT)
            .addHeader("Accept", "*/*")
            .addHeader("Connection", "keep-alive")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download file: $response")
            val body = response.body ?: throw IOException("Empty response body")
            val contentLength = body.contentLength()
            var lastLogTime = 0L

            targetFile.parentFile?.mkdirs()
            
            body.source().use { source ->
                targetFile.sink().buffer().use { sink ->
                    if (progressListener == null) {
                        val buffer = okio.Buffer()
                        var totalRead = 0L
                        while (true) {
                            val read = source.read(buffer, 8192)
                            if (read == -1L) break
                            sink.write(buffer, read)
                            totalRead += read
                            
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastLogTime > 2000) {
                                Timber.d("Download progress: ${totalRead / 1024}KB / ${contentLength / 1024}KB")
                                lastLogTime = currentTime
                            }
                        }
                    } else {
                        val buffer = okio.Buffer()
                        var totalBytesRead = 0L
                        while (true) {
                            val read = source.read(buffer, 8192)
                            if (read == -1L) break
                            sink.write(buffer, read)
                            totalBytesRead += read
                            progressListener.invoke(totalBytesRead, contentLength)
                        }
                    }
                }
            }
        }
    }

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
            val jsUrl = if (jsUrlFile.exists()) jsUrlFile.readText() else ""
            if (jsUrl.isNotEmpty()) {
                val videoId = url.substringAfter("v=").substringBefore("&").substringBefore("?")
                if (videoId.length == 11) {
                    val currentTime = System.currentTimeMillis()
                    val cached = playerResponseCache[videoId]
                    
                    val jsScriptTag = "<script src=\"$jsUrl\"></script>"

                    if (cached != null && (currentTime - cached.second) < TimeUnit.MINUTES.toMillis(5)) {
                        val playerResponseJson = json.encodeToString(cached.first)
                        val html = "<html><head>$jsScriptTag<script>var ytInitialPlayerResponse = $playerResponseJson;</script></head><body></body></html>"
                        return Response(200, "OK", mapOf("Content-Type" to listOf("text/html")), html, url)
                    }

                    try {
                        val playerResponse = runBlocking { Innertube.player(videoId) }?.getOrNull()?.also {
                            playerResponseCache[videoId] = it to currentTime
                        }

                        if (playerResponse != null) {
                            val playerResponseJson = json.encodeToString(playerResponse)
                            val html = "<html><head>$jsScriptTag<script>var ytInitialPlayerResponse = $playerResponseJson;</script></head><body></body></html>"
                            return Response(200, "OK", mapOf("Content-Type" to listOf("text/html")), html, url)
                        }
                    } catch (_: Exception) {
                    }
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
        
        if (headers.none { it.key.equals("User-Agent", ignoreCase = true) }) {
            builder.addHeader("User-Agent", Innertube.USER_AGENT)
        }

        if (headers.none { it.key.equals("X-Goog-Visitor-Id", ignoreCase = true) }) {
            Innertube.visitorData?.let { builder.addHeader("X-Goog-Visitor-Id", it) }
        }

        val requestBody = dataToSend?.toRequestBody()
            ?: if (method == "POST") ByteArray(0).toRequestBody() else null

        builder.method(method, requestBody)

        val response = client.newCall(builder.build()).execute()
        val body = response.body.string()

        if (method == "GET" && url.contains("base.js") && response.isSuccessful && body != null) {
            jsUrlFile.writeText(url)
            jsCacheFile.writeText(body)
            Timber.d("Discovered base.js: $url")
            YouTubeSessionManager.updateSession(jsUrl = url)
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
