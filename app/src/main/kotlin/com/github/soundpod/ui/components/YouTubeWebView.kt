package com.github.soundpod.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

    val decipherRequests = remember { ConcurrentHashMap<String, CompletableDeferred<String>>() }

    AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Use the exact User Agent from the provided session for perfect alignment
                settings.userAgentString = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.5 Safari/605.1.15,gzip(gfe)"
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        // Comprehensive data extraction script updated for the new session info
                        val script = """
                            (function() {
                                try {
                                    const ytConfig = window.yt?.config_ || window.ytcfg?.data_ || {};
                                    const ytcfgGet = (key) => window.ytcfg && window.ytcfg.get ? window.ytcfg.get(key) : ytConfig[key];
                                    
                                    const data = {
                                        visitorData: ytcfgGet('VISITOR_DATA') || ytConfig.VISITOR_DATA,
                                        poToken: ytcfgGet('PO_TOKEN') || ytConfig.PO_TOKEN || (window.yt && window.yt.config_ ? window.yt.config_.PO_TOKEN : null),
                                        innertubeApiKey: ytcfgGet('INNERTUBE_API_KEY') || ytConfig.INNERTUBE_API_KEY,
                                        innertubeContext: ytcfgGet('INNERTUBE_CONTEXT') || ytConfig.INNERTUBE_CONTEXT,
                                        clientName: ytcfgGet('INNERTUBE_CLIENT_NAME') || ytConfig.INNERTUBE_CLIENT_NAME,
                                        clientVersion: ytcfgGet('INNERTUBE_CLIENT_VERSION') || ytConfig.INNERTUBE_CLIENT_VERSION,
                                        jsUrl: window.ytplayer && window.ytplayer.config && window.ytplayer.config.assets ? window.ytplayer.config.assets.js : null
                                    };
                                    
                                    // Deep search for PO_TOKEN if still missing
                                    if (!data.poToken) {
                                        const scripts = document.getElementsByTagName('script');
                                        for (let s of scripts) {
                                            const text = s.innerText || s.textContent;
                                            if (text && text.includes('PO_TOKEN')) {
                                                const match = text.match(/"PO_TOKEN"\s*:\s*"([^"]+)"/);
                                                if (match) {
                                                    data.poToken = match[1];
                                                    break;
                                                }
                                            }
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
                                                // Try Rhino first, fallback to WebView
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

                        // Initialize BotGuard and decipher script
                        view?.evaluateJavascript(BotGuard.HTML) { }
                        
                        injectDecipherScript(view)
                    }

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun onDecipherResult(requestId: String, result: String) {
                        decipherRequests.remove(requestId)?.complete(result)
                    }
                }, "SoundPodBridge")

                loadUrl("https://music.youtube.com")
            }
        }
    )
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
        // This will throw if the WebView package is missing or disabled
        CookieManager.getInstance()
        true
    } catch (_: Exception) {
        false
    }
}
