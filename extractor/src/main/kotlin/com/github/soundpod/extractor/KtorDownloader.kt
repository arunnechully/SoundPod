package com.github.soundpod.extractor

import com.github.innertube.Innertube
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

class KtorDownloader : Downloader() {
    override fun execute(request: Request): Response {
        val url = request.url()
        val method = request.httpMethod()
        val headers = request.headers()
        // val body = request.data // Troubleshooting data error
        val body: ByteArray? = null 

        return runBlocking {
            try {
                val response = Innertube.client.request(url) {
                    this.method = HttpMethod.parse(method)
                    headers.forEach { (key, values) ->
                        values.forEach { value -> header(key, value) }
                    }
                    if (body != null) {
                        setBody(body)
                    }
                    attributes.put(Innertube.Attributes.UseCookies, true)
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
