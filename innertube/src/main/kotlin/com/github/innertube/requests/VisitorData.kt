package com.github.innertube.requests

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import com.github.innertube.Innertube
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

suspend fun Innertube.getSwJsData() = client.get("https://music.youtube.com/sw.js_data")

suspend fun Innertube.visitorData(): Result<String> = runCatching {
    Json.parseToJsonElement(getSwJsData().bodyAsText().substring(5))
        .jsonArray[0]
        .jsonArray[2]
        .jsonArray.first {
            (it as? JsonPrimitive)?.contentOrNull?.let { candidate ->
                Regex("^Cg[t|s]").containsMatchIn(candidate)
            } == true
        }
        .jsonPrimitive.content
}