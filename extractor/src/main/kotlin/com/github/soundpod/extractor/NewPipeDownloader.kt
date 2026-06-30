package com.github.soundpod.extractor

import com.github.innertube.PreferIpv4Dns
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.File
import java.util.concurrent.TimeUnit

class NewPipeDownloader private constructor(cacheDir: File) : Downloader() {
    private val client = OkHttpClient.Builder()
        .dns(PreferIpv4Dns)
        .cache(Cache(File(cacheDir, "newpipe_cache"), 10 * 1024 * 1024))
        .dispatcher(Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 20
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val jsCacheFile = File(cacheDir, "base_js_content")
    private val jsUrlFile = File(cacheDir, "base_js_url")

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

        val headers = request.headers().toMutableMap()
        val dataToSend = request.dataToSend()

        val builder = okhttp3.Request.Builder()
            .url(url)

        headers.forEach { (name, values) ->
            values.forEach { value ->
                builder.addHeader(name, value)
            }
        }
        
        if (headers.none { it.key.equals("User-Agent", ignoreCase = true) }) {
            builder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
        }

        if (headers.none { it.key.equals("Cookie", ignoreCase = true) }) {
            builder.addHeader("Cookie", "SOCS=CAESEwgDEgk0ODE3Nzk3MjQaAmVuIAEaBgiA_LyaBg")
        }
        
        if (headers.none { it.key.equals("Accept-Language", ignoreCase = true) }) {
            builder.addHeader("Accept-Language", "en-US,en;q=0.9")
        }

        if (headers.none { it.key.equals("Accept", ignoreCase = true) }) {
            builder.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
        }
        
        builder.addHeader("Sec-Fetch-Dest", "document")
        builder.addHeader("Sec-Fetch-Mode", "navigate")
        builder.addHeader("Sec-Fetch-Site", "none")
        builder.addHeader("Sec-Fetch-User", "?1")
        builder.addHeader("Upgrade-Insecure-Requests", "1")

        val requestBody = dataToSend?.toRequestBody()
            ?: if (method == "POST") ByteArray(0).toRequestBody() else null

        builder.method(method, requestBody)

        val response = client.newCall(builder.build()).execute()
        val body = response.body.string()

        if (method == "GET" && url.contains("base.js") && response.isSuccessful) {
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
