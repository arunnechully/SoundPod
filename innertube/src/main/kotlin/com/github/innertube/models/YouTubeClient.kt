package com.github.innertube.models

import com.github.innertube.Innertube
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
        clientVersion = "1.20240214.01.00",
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        platform = "DESKTOP",
        osVersion = "10",
        clientId = "67"
    ),
    ANDROID_VR(
        clientName = "ANDROID_VR",
        clientVersion = "1.71.26",
        userAgent = "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 14; eureka-user Build/SQ3A.220605.009.A1) gzip",
        osVersion = "14",
        clientId = "28"
    );

    fun toContext(
        localized: Boolean = true,
        visitorData: String? = null,
        gl: String? = null,
        hl: String? = null,
        includeThirdParty: Boolean = false
    ): Context {
        return Context(
            client = Context.Client(
                clientName = clientName,
                clientVersion = clientVersion,
                clientId = clientId,
                osVersion = osVersion ?: "13",
                platform = platform ?: when (this) {
                    ANDROID_VR -> "MOBILE"
                    else -> "DESKTOP"
                },
                userAgent = userAgent,
                gl = gl ?: if (localized) Locale.getDefault().country.takeIf { it.length == 2 } ?: "US" else "US",
                hl = hl ?: "en",
                visitorData = visitorData ?: Innertube.visitorData ?: ""
            ),
            thirdParty = if (includeThirdParty) Context.ThirdParty(embedUrl = "https://www.youtube.com/watch?v=") else null
        )
    }
}
