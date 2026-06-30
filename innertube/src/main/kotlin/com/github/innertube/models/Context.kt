package com.github.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Context(
    val client: Client,
    val thirdParty: ThirdParty? = null
) {
    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val clientId: String,
        val osVersion: String?,
        val platform: String?,
        val userAgent: String,
        val gl: String,
        val hl: String,
        val visitorData: String?
    )

    @Serializable
    data class ThirdParty(
        val embedUrl: String
    )
}