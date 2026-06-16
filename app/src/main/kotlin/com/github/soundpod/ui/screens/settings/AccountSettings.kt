package com.github.soundpod.ui.screens.settings

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.github.innertube.Innertube
import com.github.soundpod.R
import com.github.soundpod.service.YouTubeSessionManager
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.navigation.SettingsDestinations

@Composable
fun AccountSettingsContent(onOptionClick: (String) -> Unit) {
    val isLoggedIn = Innertube.isLoggedIn

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsGroup(title = stringResource(R.string.account)) {
            if (!isLoggedIn) {
                SettingsColumn(
                    title = stringResource(R.string.sign_in),
                    description = stringResource(R.string.sign_in_description),
                    icon = IconSource.Vector(Icons.AutoMirrored.Filled.Login),
                    onClick = { onOptionClick(SettingsDestinations.LOGIN) },
                )
            } else {
                SettingsColumn(
                    title = stringResource(R.string.youtube_account),
                    description = stringResource(R.string.youtube_account_description),
                    icon = IconSource.Vector(Icons.Default.AccountCircle),
                )
                SettingsColumn(
                    title = stringResource(R.string.sign_out),
                    description = stringResource(R.string.sign_out_description),
                    icon = IconSource.Vector(Icons.AutoMirrored.Filled.Logout),
                    onClick = {
                        CookieManager.getInstance().removeAllCookies(null)
                        YouTubeSessionManager.updateSession(cookies = "")
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginSettingsContent(onBack: () -> Unit) {
    var isLoading by remember { mutableStateOf(value = true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36"
                    }
                    
                    webChromeClient = android.webkit.WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                            val url = request?.url?.toString() ?: ""
                            if (url.contains("googlevideo.com") || url.contains("/videoplayback")) {
                                return WebResourceResponse("text/plain", "UTF-8", null)
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    const media = document.querySelectorAll('video, audio');
                                    media.forEach(m => { m.muted = true; m.pause(); });
                                })();
                            """.trimIndent(),
                                null,
                            )

                            val cookies = CookieManager.getInstance().getCookie(url)
                            if ((cookies?.contains("__Secure-3PAPISID") == true) || (cookies?.contains("SAPISID") == true)) {
                                YouTubeSessionManager.updateSession(cookies = cookies)
                                if (url?.contains("music.youtube.com") == true && !url.contains("login")) {
                                    onBack()
                                }
                            }
                        }
                    }
                    loadUrl("https://music.youtube.com/")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
