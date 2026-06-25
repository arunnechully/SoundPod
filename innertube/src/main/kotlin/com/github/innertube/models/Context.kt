package com.github.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Context(
    val client: Client,
    val user: User? = null,
    val request: Request? = null,
    val clickTracking: ClickTracking? = null,
    val thirdParty: ThirdParty? = null
) {
    @Serializable
    data class Client(
        val clientName: String,
        val clientVersion: String,
        val clientId: String? = null,
        val osVersion: String? = null,
        val osName: String? = null,
        val platform: String? = null,
        val userAgent: String,
        val gl: String? = null,
        val hl: String? = null,
        val visitorData: String? = null,
        val remoteHost: String? = null,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val clientFormFactor: String? = null,
        val configInfo: ConfigInfo? = null,
        val browserName: String? = null,
        val browserVersion: String? = null,
        val acceptHeader: String? = null,
        val deviceExperimentId: String? = null,
        val rolloutToken: String? = null
    )

    @Serializable
    data class ConfigInfo(
        val appInstallData: String? = null
    )

    @Serializable
    data class User(
        val lockedSafetyMode: Boolean? = null
    )

    @Serializable
    data class Request(
        val useSsl: Boolean? = null
    )

    @Serializable
    data class ClickTracking(
        val clickTrackingParams: String? = null
    )

    @Serializable
    data class ThirdParty(
        val embedUrl: String
    )
}