package com.github.soundpod.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.innertube.BotGuard
import com.github.soundpod.service.YouTubeDecipherer
import com.github.soundpod.service.YouTubeSessionManager
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebView() {
    if (!isWebViewAvailable(LocalContext.current)) {
        Log.w("SoundPod-WebView", "WebView not available, skipping background WebView initialization")
        return
    }

    val needsConsent by YouTubeSessionManager.needsConsent.collectAsState()
    val decipherRequests = remember { ConcurrentHashMap<String, CompletableDeferred<String>>() }
    var isPageLoading by remember { mutableStateOf(true) }

    if (needsConsent) {
        Dialog(
            onDismissRequest = { YouTubeSessionManager.setNeedsConsent(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp, horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                setupWebView(this, decipherRequests) { loading ->
                                    isPageLoading = loading
                                }
                                loadUrl("https://music.youtube.com")
                            }
                        }
                    )
                    
                    if (isPageLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(
                        onClick = { YouTubeSessionManager.setNeedsConsent(false) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    } else {
        AndroidView(
            modifier = Modifier.size(1.dp),
            factory = { context ->
                WebView(context).apply {
                    setupWebView(this, decipherRequests) { }
                    loadUrl("https://music.youtube.com")
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupWebView(
    webView: WebView, 
    decipherRequests: ConcurrentHashMap<String, CompletableDeferred<String>>,
    onLoadingStatusChanged: (Boolean) -> Unit
) {
    webView.settings.javaScriptEnabled = true
    webView.settings.domStorageEnabled = true
    webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    
    webView.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onLoadingStatusChanged(true)
            if (url?.contains("consent.youtube.com") == true) {
                Log.i("SoundPod-WebView", "Consent page detected: $url")
                YouTubeSessionManager.setNeedsConsent(true)
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onLoadingStatusChanged(false)
            
            // If we are back on music.youtube.com and not on a consent page, we likely have what we need
            if (url?.contains("music.youtube.com") == true && !url.contains("consent")) {
                // We don't immediately set needsConsent = false here, 
                // we wait for the script to successfully extract data.
            }

            val script = """
                (function() {
                    try {
                        const ytConfig = window.yt?.config_ || window.ytcfg?.data_ || {};
                        const ytcfgGet = (key) => {
                            if (window.ytcfg && window.ytcfg.get) return window.ytcfg.get(key);
                            return ytConfig[key];
                        };
                        
                        const data = {
                            visitorData: ytcfgGet('VISITOR_DATA') || ytConfig.VISITOR_DATA,
                            poToken: ytcfgGet('PO_TOKEN') || ytConfig.PO_TOKEN || (window.yt && window.yt.config_ ? window.yt.config_.PO_TOKEN : null),
                            innertubeApiKey: ytcfgGet('INNERTUBE_API_KEY') || ytConfig.INNERTUBE_API_KEY,
                            innertubeContext: ytcfgGet('INNERTUBE_CONTEXT') || ytConfig.INNERTUBE_CONTEXT,
                            clientName: ytcfgGet('INNERTUBE_CLIENT_NAME') || ytConfig.INNERTUBE_CLIENT_NAME,
                            clientVersion: ytcfgGet('INNERTUBE_CLIENT_VERSION') || ytConfig.INNERTUBE_CLIENT_VERSION,
                            jsUrl: (window.ytplayer && window.ytplayer.config && window.ytplayer.config.assets) ? window.ytplayer.config.assets.js : null
                        };
                        
                        const scripts = document.getElementsByTagName('script');
                        for (let s of scripts) {
                            const text = s.innerText || s.textContent;
                            if (!text) continue;
                            
                            if (!data.visitorData && text.includes('VISITOR_DATA')) {
                                const match = text.match(/"VISITOR_DATA"\s*:\s*"([^"]+)"/);
                                if (match) data.visitorData = match[1];
                            }
                            if (!data.poToken && text.includes('PO_TOKEN')) {
                                const match = text.match(/"PO_TOKEN"\s*:\s*"([^"]+)"/);
                                if (match) data.poToken = match[1];
                            }
                            if (!data.jsUrl && text.includes('base.js')) {
                                const match = text.match(/"js"\s*:\s*"([^"]+base\.js)"/) || text.match(/\/s\/player\/[a-zA-Z0-9]+\/player_ias\.vflset\/[a-zA-Z0-9_\/.]+\/base\.js/);
                                if (match) data.jsUrl = Array.isArray(match) ? match[match.length-1] : (typeof match === 'string' ? match : null);
                            }
                        }

                        return JSON.stringify(data);
                    } catch (e) {
                        return JSON.stringify({ error: e.message });
                    }
                })()
            """.trimIndent()

            view?.evaluateJavascript(script) { sessionDataJson ->
                try {
                    val sessionData = sessionDataJson?.replace("^\"|\"$".toRegex(), "")?.replace("\\\"", "\"")
                    if (sessionData != null) {
                        val json = org.json.JSONObject(sessionData)
                        val visitorData = json.optString("visitorData").takeIf { it != "null" && it.isNotBlank() }
                        val poToken = json.optString("poToken").takeIf { it != "null" && it.isNotBlank() }
                        val webApiKey = json.optString("innertubeApiKey").takeIf { it != "null" && it.isNotBlank() }
                        val clientVersion = json.optString("clientVersion").takeIf { it != "null" && it.isNotBlank() }
                        val jsUrl = json.optString("jsUrl").takeIf { it != "null" && it.isNotBlank() }
                        
                        if (visitorData != null) {
                            // MAGIC MOMENT: If we successfully got visitorData, we can close the consent dialog!
                            YouTubeSessionManager.setNeedsConsent(false)

                            val cookies = CookieManager.getInstance().getCookie(url)
                            Log.d("SoundPod-WebView", "Extracted Session: VisitorData=${visitorData.take(20)}..., POToken=${poToken ?: "null"}, JSUrl=$jsUrl")
                            
                            YouTubeSessionManager.updateSession(
                                visitorData = visitorData,
                                poToken = poToken,
                                apiKey = webApiKey,
                                clientVersion = clientVersion,
                                jsUrl = jsUrl,
                                cookies = cookies,
                                decipher = { nParam ->
                                    val result = YouTubeDecipherer.decipher(nParam)
                                    if (result != nParam) {
                                        Log.d("SoundPod-WebView", "Successfully deciphered via Rhino: $nParam -> $result")
                                        result
                                    } else {
                                        Log.w("SoundPod-WebView", "Rhino failed, falling back to WebView for deciphering")
                                        val deferred = CompletableDeferred<String>()
                                        val requestId = System.currentTimeMillis().toString() + nParam
                                        decipherRequests[requestId] = deferred
                                        
                                        view.post {
                                            view.evaluateJavascript(
                                                "if (typeof decipherNParam === 'function') { " +
                                                "  decipherNParam('$nParam', '$requestId'); " +
                                                "} else { " +
                                                "  console.error('decipherNParam not ready'); " +
                                                "  SoundPodBridge.onDecipherResult('$requestId', '$nParam'); " +
                                                "}"
                                            ) { }
                                        }
                                        deferred.await()
                                    }
                                }
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SoundPod-WebView", "Failed to parse session data", e)
                }
            }

            view?.evaluateJavascript(BotGuard.HTML) { }
            injectDecipherScript(view)
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            return super.shouldInterceptRequest(view, request)
        }
    }

    webView.addJavascriptInterface(object {
        @android.webkit.JavascriptInterface
        fun onDecipherResult(requestId: String, result: String) {
            decipherRequests.remove(requestId)?.complete(result)
        }
    }, "SoundPodBridge")
}

private fun injectDecipherScript(webView: WebView?) {
    val script = """
        function decipherNParam(n, requestId) {
            let result = n; 
            if (window.decipherFunction) {
                try { result = window.decipherFunction(n); } catch(e) { console.error(e); }
            }
            SoundPodBridge.onDecipherResult(requestId, result);
        }
    """.trimIndent()
    webView?.evaluateJavascript(script) { }
}

fun isWebViewAvailable(context: android.content.Context): Boolean {
    return try {
        CookieManager.getInstance()
        true
    } catch (_: Exception) {
        false
    }
}
