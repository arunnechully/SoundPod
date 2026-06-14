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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.innertube.BotGuard
import com.github.soundpod.service.YouTubeSessionManager
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebView() {
    val decipherRequests = remember { ConcurrentHashMap<String, CompletableDeferred<String>>() }

    AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        // Extract cookies and visitor data
                        val cookies = CookieManager.getInstance().getCookie(url)
                        view?.evaluateJavascript("(function() { return window.yt?.config_?.VISITOR_DATA || (window.ytcfg && ytcfg.get ? ytcfg.get('VISITOR_DATA') : null); })()") { visitorData ->
                            val cleanVisitorData = visitorData?.replace("\"", "")
                            if (cleanVisitorData != "null" && !cleanVisitorData.isNullOrBlank()) {
                                Log.d("SoundPod-WebView", "Extracted VisitorData: $cleanVisitorData")
                                YouTubeSessionManager.updateSession(
                                    visitorData = cleanVisitorData,
                                    cookies = cookies,
                                    decipher = { nParam ->
                                        val deferred = CompletableDeferred<String>()
                                        val requestId = System.currentTimeMillis().toString() + nParam
                                        decipherRequests[requestId] = deferred
                                        
                                        view.post {
                                            view.evaluateJavascript(
                                                "if (typeof decipherNParam === 'function') { " +
                                                "  decipherNParam('$nParam', '$requestId'); " +
                                                "} else { " +
                                                "  console.error('decipherNParam not ready'); " +
                                                "  SoundPodBridge.onDecipherResult('$requestId', '$nParam'); " + // Fallback
                                                "}"
                                            ) { }
                                        }
                                        deferred.await()
                                    }
                                )
                            }
                        }

                        // Initialize BotGuard and decipher script
                        view?.evaluateJavascript(BotGuard.HTML) { }
                        
                        // Try to find the decipher function in the page
                        view?.evaluateJavascript("""
                            (function() {
                                if (window.decipherNParam) return;
                                // Basic heuristic to find the 'n' decipher function
                                // This is still a bit of an "old trick" and might need adjustment 
                                // based on how YouTube currently serves base.js
                                console.log("SoundPod: Searching for decipher function...");
                            })();
                        """.trimIndent()) { }

                        injectDecipherScript(view)
                    }

                    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                        // Potential to intercept base.js here to extract decipher logic more robustly
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
            // Placeholder logic: in reality, we'd call the function from base.js
            // For now, returning as is or with a simple transformation to avoid blocking
            let result = n; 
            if (window.decipherFunction) {
                try { result = window.decipherFunction(n); } catch(e) { console.error(e); }
            }
            SoundPodBridge.onDecipherResult(requestId, result);
        }
    """.trimIndent()
    webView?.evaluateJavascript(script) { }
}
