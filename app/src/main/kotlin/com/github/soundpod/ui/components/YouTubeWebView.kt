package com.github.soundpod.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
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

private class SoundPodJsBridge(
    private val decipherRequests: ConcurrentHashMap<String, CompletableDeferred<String>>
) {
    @JavascriptInterface
    fun onDecipherResult(requestId: String, result: String) {
        decipherRequests.remove(requestId)?.complete(result)
    }

    @JavascriptInterface
    fun onPoTokenResult(poToken: String) {
        Log.d("SoundPod-WebView", "Received poToken: $poToken")
        YouTubeSessionManager.updateSession(poToken = poToken)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebView() {
    // Hold pending requests in state
    val decipherRequests = remember { ConcurrentHashMap<String, CompletableDeferred<String>>() }

    AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // Using a modern mobile User-Agent
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

                //Attach the explicitly defined bridge
                addJavascriptInterface(SoundPodJsBridge(decipherRequests), "SoundPodBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)

                        // Ensure cookies are flushed to storage
                        CookieManager.getInstance().flush()
                        val cookies = CookieManager.getInstance().getCookie(url)

                        //Safer JavaScript evaluation with proper null checks
                        val extractVisitorDataJs = """
                            (function() { 
                                return window.yt?.config_?.VISITOR_DATA || 
                                       (window.ytcfg && ytcfg.get ? ytcfg.get('VISITOR_DATA') : null); 
                            })();
                        """.trimIndent()

                        view.evaluateJavascript(extractVisitorDataJs) { visitorData ->
                            val cleanVisitorData = visitorData?.replace("\"", "")

                            if (!cleanVisitorData.isNullOrBlank() && cleanVisitorData != "null") {
                                Log.d("SoundPod-WebView", "Extracted VisitorData: $cleanVisitorData")

                                YouTubeSessionManager.updateSession(
                                    visitorData = cleanVisitorData,
                                    cookies = cookies,
                                    decipher = { nParam ->
                                        val deferred = CompletableDeferred<String>()
                                        val requestId = "${System.currentTimeMillis()}_$nParam"
                                        decipherRequests[requestId] = deferred

                                        val decipherInvokeJs = """
                                            if (typeof decipherNParam === 'function') {
                                                decipherNParam('$nParam', '$requestId');
                                            } else {
                                                console.error('decipherNParam not ready');
                                                SoundPodBridge.onDecipherResult('$requestId', '$nParam');
                                            }
                                        """.trimIndent()

                                        view.post {
                                            view.evaluateJavascript(decipherInvokeJs, null)
                                        }

                                        deferred.await()
                                    }
                                )
                            }
                        }
                        try {
                            view.evaluateJavascript(BotGuard.JS, null)
                            
                            val generatePoTokenJs = """
                                (async function() {
                                    try {
                                        const challenge = window.yt?.config_?.WEB_PLAYER_CONTEXT_CONFIG_ID_WEB_PLAYER_BOTGUARD_CHALLENGE || 
                                                        (window.ytcfg && ytcfg.get ? ytcfg.get('WEB_PLAYER_CONTEXT_CONFIG_ID_WEB_PLAYER_BOTGUARD_CHALLENGE') : null);
                                        
                                        if (challenge) {
                                            const result = await runBotGuard(challenge);
                                            if (result && result.botguardResponse) {
                                                SoundPodBridge.onPoTokenResult(result.botguardResponse);
                                            }
                                        }
                                    } catch (e) {}
                                })();
                            """.trimIndent()
                            
                            view.evaluateJavascript(generatePoTokenJs, null)
                        } catch (e: Exception) {
                            Log.e("SoundPod-WebView", "Failed to inject BotGuard script", e)
                        }

                        // Try to find the decipher function
                        injectDecipherScript(view)
                    }
                }

                loadUrl("https://music.youtube.com")
            }
        },
        onRelease = { webView ->
            decipherRequests.values.forEach { it.cancel() }
            decipherRequests.clear()

            webView.removeJavascriptInterface("SoundPodBridge")
            webView.stopLoading()
            webView.destroy()
        }
    )
}

private fun injectDecipherScript(webView: WebView) {
    val script = """
        function decipherNParam(n, requestId) {
            let result = n; 
            if (window.decipherFunction) {
                try { 
                    result = window.decipherFunction(n); 
                } catch(e) { 
                    console.error(e); 
                }
            }
            SoundPodBridge.onDecipherResult(requestId, result);
        }
    """.trimIndent()
    webView.evaluateJavascript(script, null)
}