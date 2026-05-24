package com.github.innertube.models

import com.github.innertube.Innertube
import java.util.Locale

class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val platform: String? = null,
    val osVersion: String? = null,
    visitorData: String? = null
) {
    fun toContext(
        localized: Boolean = false, // Changed default to false. to force US/en and bypass strict regions
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
        val MAC_SAFARI_WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260517.17.00",
            clientId = "67",
            platform = "DESKTOP",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.4 Safari/605.1.15,gzip(gfe)",
            osVersion = "10_15_7",
            visitorData = Innertube.visitorData
            )
        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260114.03.00",
            clientId = "67",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0",
            platform = "DESKTOP",
            visitorData = Innertube.visitorData
        )
        val ANDROID_VR = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.71.26",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osVersion = "12L",
            visitorData = Innertube.visitorData
        )

        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15",
            platform = "TV",
            visitorData = Innertube.visitorData
        )
    }
}