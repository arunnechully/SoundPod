package com.github.soundpod.extractor

import com.github.innertube.Innertube
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

class KtorDownloader : Downloader() {
    companion object {
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
    }

    private val client = HttpClient(OkHttp) {
        followRedirects = true
    }

    override fun execute(request: Request): Response {
        val url = request.url()
        val method = request.httpMethod()
        val headers = request.headers()
        val body = request.dataToSend()

        return runBlocking {
            try {
                val response = client.request(url) {
                    this.method = HttpMethod.parse(method)
                    headers.forEach { (key, values) ->
                        values.forEach { value -> header(key, value) }
                    }
                    
                    if (headers.none { it.key.equals("User-Agent", ignoreCase = true) }) {
                        header("User-Agent", DEFAULT_USER_AGENT)
                    }
                    
                    if (body != null && body.isNotEmpty()) {
                        setBody(body)
                    }
                    
                    if (headers.none { it.key.equals("Cookie", ignoreCase = true) }) {
                        Innertube.cookies?.let { header("Cookie", it) }
                    }
                }

                val responseBody = response.bodyAsText()
                val responseHeaders = response.headers.toMap()

                Response(
                    response.status.value,
                    response.status.description,
                    responseHeaders,
                    responseBody,
                    response.request.url.toString()
                )
            } catch (e: Exception) {
                throw org.schabi.newpipe.extractor.exceptions.ReCaptchaException("Ktor request failed: ${e.message}", url)
            }
        }
    }
}
