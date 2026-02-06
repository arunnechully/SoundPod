package com.github.innertube.models

import java.util.Locale

class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val platform: String? = null,
    val osVersion: String? = null
) {
    fun toContext(
        localized: Boolean = true,
        visitorData: String? = null
    ) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            clientId = clientId,
            osVersion = osVersion,
            platform = platform,
            userAgent = userAgent,
            gl = if (localized) Locale.getDefault().country else "US",
            hl = if (localized) Locale.getDefault().toLanguageTag() else "en",
            visitorData = visitorData
        )
    )


    companion object {
        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260114.03.00",
            clientId = "67",
            platform = "DESKTOP",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
        )

        val ANDROID_VR = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.71.26",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osVersion = "12L"
        )

        /* val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "20.10.4",
            clientId = "5",
            platform = "MOBILE",
            userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
            osVersion = "18.3.2.22D82"
        ) */

        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            platform = "TV",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15"
        )
    }
}