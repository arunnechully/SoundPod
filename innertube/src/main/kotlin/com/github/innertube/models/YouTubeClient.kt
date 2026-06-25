package com.github.innertube.models

import com.github.innertube.Innertube
import java.util.Locale

@Suppress("SpellCheckingInspection")
enum class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    private val _userAgent: String,
    val platform: String? = null,
    val osVersion: String? = null,
    val clientId: String? = null
) {
    WEB_REMIX(
        clientName = "WEB_REMIX",
        clientVersion = "1.20240214.01.00",
        _userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        platform = "DESKTOP",
        osVersion = "10",
        clientId = "67"
    ),
    TVHTML5_SIMPLY_EMBEDDED_PLAYER(
        clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
        clientVersion = "3.20250615.01.00",
        _userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15",
        platform = "TV",
        clientId = "85"
    ),
    ANDROID_VR(
        clientName = "ANDROID_VR",
        clientVersion = "1.71.26",
        _userAgent = "com.google.android.apps.youtube.vr.oculus/1.71.26 (Linux; U; Android 14; eureka-user Build/SQ3A.220605.009.A1) gzip",
        osVersion = "14",
        clientId = "28"
    ),
    ANDROID_TESTSUITE(
        clientName = "ANDROID_TESTSUITE",
        clientVersion = "1.9.30.1",
        _userAgent = "com.google.android.youtube.testsuite/1.9.30.1 (Linux; U; Android 14; en_US) gzip",
        osVersion = "14",
        clientId = "30"
    ),
    WEB_EMBEDDED_PLAYER(
        clientName = "WEB_EMBEDDED_PLAYER",
        clientVersion = "1.20260615.01.00",
        _userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.5 Safari/605.1.15,gzip(gfe)"
    ),
    DYNAMIC(
        clientName = "DYNAMIC",
        clientVersion = "1.0",
        _userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"
    );

    val userAgent: String
        get() = if (this == DYNAMIC) Innertube.context?.client?.userAgent ?: _userAgent else _userAgent

    fun toContext(
        localized: Boolean = true,
        visitorData: String? = null,
        gl: String? = null,
        hl: String? = null,
        includeThirdParty: Boolean = false
    ): Context {
        if (this == DYNAMIC && Innertube.context != null) {
            val baseContext = Innertube.context!!
            // Override visitorData if provided
            val updatedClient = if (visitorData != null || Innertube.visitorData != null) {
                baseContext.client.copy(visitorData = visitorData ?: Innertube.visitorData)
            } else {
                baseContext.client
            }
            
            return baseContext.copy(
                client = updatedClient,
                thirdParty = if (includeThirdParty) Context.ThirdParty(embedUrl = "https://www.youtube.com/watch?v=") else baseContext.thirdParty
            )
        }

        return Context(
            client = Context.Client(
                clientName = if (this == DYNAMIC) Innertube.clientName ?: "WEB_REMIX" else clientName,
                clientVersion = if (this == DYNAMIC) Innertube.clientVersion ?: clientVersion else clientVersion,
                clientId = clientId ?: (if (this == DYNAMIC) "67" else null),
                osVersion = osVersion ?: "13",
                platform = platform ?: when (this) {
                    ANDROID_VR, ANDROID_TESTSUITE -> "MOBILE"
                    TVHTML5_SIMPLY_EMBEDDED_PLAYER -> "TV"
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
