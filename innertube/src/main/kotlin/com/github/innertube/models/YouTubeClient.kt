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
        clientVersion = "1.20250416.01.00",
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
        platform = "DESKTOP",
        clientId = "67"
    ),
    IOS(
        clientName = "IOS",
        clientVersion = "19.45.4",
        userAgent = "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1 like Mac OS X; en_US)"
    ),
    MWEB(
        clientName = "MWEB",
        clientVersion = "2.20250416.01.00",
        userAgent = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"
    ),
    ANDROID(
        clientName = "ANDROID",
        clientVersion = "20.10.38",
        userAgent = "com.google.android.youtube/20.10.38 (Linux; U; Android 14; en_US; SM-S928B Build/UP1A.231005.007)"
    ),
    ANDROID_MUSIC(
        clientName = "ANDROID_MUSIC",
        clientVersion = "7.07.51",
        userAgent = "com.google.android.apps.youtube.music/7.07.51 (Linux; U; Android 14; en_US; SM-S928B Build/UP1A.231005.007)"
    ),
    ANDROID_TESTSUITE(
        clientName = "ANDROID_TESTSUITE",
        clientVersion = "1.9.31",
        userAgent = "com.google.android.youtube.testsuite/1.9.31 (Linux; U; Android 14; en_US; SM-S928B Build/UP1A.231005.007)"
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
    MAC_SAFARI_WEB_REMIX(
        clientName = "WEB_REMIX",
        clientVersion = "1.20250416.01.00",
        userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15,gzip(gfe)",
        platform = "DESKTOP",
        osVersion = "10_15_7",
        clientId = "67"
    );
    fun toContext(
        localized: Boolean = true,
        visitorData: String? = null,
        gl: String? = null,
        hl: String? = null
    ) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            clientId = clientId ?: when (this) {
                IOS -> "5"
                ANDROID -> "1"
                ANDROID_MUSIC -> "21"
                MWEB -> "2"
                else -> "67"
            },
            osVersion = osVersion ?: when (this) {
                ANDROID, ANDROID_MUSIC, ANDROID_TESTSUITE -> "14"
                IOS -> "17.6"
                MWEB -> "14"
                else -> "13"
            },
            platform = platform ?: when (this) {
                IOS -> "MOBILE"
                ANDROID -> "MOBILE"
                ANDROID_MUSIC -> "MOBILE"
                ANDROID_VR -> "MOBILE"
                MWEB -> "MOBILE"
                TVHTML5_SIMPLY_EMBEDDED_PLAYER -> "TV"
                else -> "DESKTOP"
            },
            userAgent = userAgent,
            gl = gl ?: if (localized) Locale.getDefault().country.takeIf { it.length == 2 } ?: "US" else "US",
            hl = hl ?: if (localized) Locale.getDefault().language.ifBlank { "en" } else "en",
            visitorData = visitorData ?: ""
        )
    )
}