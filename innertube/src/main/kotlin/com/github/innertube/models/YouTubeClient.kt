package com.github.innertube.models

import java.util.Locale

@Suppress("SpellCheckingInspection")
enum class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val userAgent: String,
    val platform: String? = null,
    val osVersion: String? = null,
    val clientId: String? = null
) {
    WEB_REMIX(
        clientName = "WEB_REMIX",
        clientVersion = "1.20260615.01.00",
        userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.5 Safari/605.1.15,gzip(gfe)",
        platform = "DESKTOP",
        osVersion = "10_15_7",
        clientId = "67"
    ),
    TVHTML5_SIMPLY_EMBEDDED_PLAYER(
        clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
        clientVersion = "2.0",
        userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15",
        platform = "TV",
        clientId = "85"
    ),
    ANDROID_VR(
        clientName = "ANDROID_VR",
        clientVersion = "1.71.26",
        userAgent = "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 14; eureka-user Build/SQ3A.220605.009.A1) gzip",
        osVersion = "14",
        clientId = "28"
    ),
    ANDROID_TESTSUITE(
        clientName = "ANDROID_TESTSUITE",
        clientVersion = "1.9.30.1",
        userAgent = "com.google.android.youtube.testsuite/1.9.30.1 (Linux; U; Android 14; en_US) gzip",
        osVersion = "14",
        clientId = "30"
    );

    fun toContext(
        localized: Boolean = true,
        visitorData: String? = null,
        gl: String? = null,
        hl: String? = null,
        includeThirdParty: Boolean = false
    ) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            clientId = clientId ?: "67",
            osVersion = osVersion ?: "13",
            platform = platform ?: when (this) {
                ANDROID_VR, ANDROID_TESTSUITE -> "MOBILE"
                TVHTML5_SIMPLY_EMBEDDED_PLAYER -> "TV"
                else -> "DESKTOP"
            },
            userAgent = userAgent,
            gl = gl ?: if (localized) Locale.getDefault().country.takeIf { it.length == 2 } ?: "US" else "US",
            hl = hl ?: if (localized) Locale.getDefault().language.ifBlank { "en" } else "en",
            visitorData = visitorData ?: ""
        ),
        thirdParty = if (includeThirdParty) Context.ThirdParty(embedUrl = "https://www.youtube.com/watch?v=") else null
    )
}
