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
        val ANDROID_MUSIC = YouTubeClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "6.02.53",
            clientId = "21",
            platform = "MOBILE",
            userAgent = "com.google.android.apps.youtube.music/6.02.53 (Linux; U; Android 13; en_US) gzip",
            osVersion = "13"
        )

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260114.03.00",
            clientId = "67",
            platform = "DESKTOP",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
        )

        val WEB = YouTubeClient(
            clientName = "WEB",
            clientVersion = "2.20240114.01.00",
            clientId = "1",
            platform = "DESKTOP",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )

        val ANDROID_VR = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.71.26",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osVersion = "12L"
        )

        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            platform = "TV",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15"
        )

        val ANDROID = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "19.05.36",
            clientId = "2",
            userAgent = "com.google.android.youtube/19.05.36 (Linux; U; Android 11; en_US) gzip",
            osVersion = "11"
        )

        val MWEB = YouTubeClient(
            clientName = "MWEB",
            clientVersion = "2.20240114.01.00",
            clientId = "1",
            platform = "MOBILE",
            userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        )
    }
}