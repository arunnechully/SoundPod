package com.github.innertube.requests

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import com.github.innertube.Innertube

private val visitorDataRegex = Regex(""""visitorData"\s*:\s*"([^"]+)"""")

suspend fun Innertube.visitorData(): Result<String> = runCatching {
    val responseText = client.get("https://music.youtube.com/") {
        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        header("Accept-Language", "en-US,en;q=0.9")
    }.bodyAsText()
    val match = visitorDataRegex.find(responseText)
    val token = match?.groupValues?.get(1)

    if (token != null && Regex("^Cg([ts])").containsMatchIn(token)) {
        token
    } else {
        "CgtWT0xjS1FfMDRzSSjplbKwBg%3D%3D"
    }
}