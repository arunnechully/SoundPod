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

    @JavascriptInterface
    fun log(message: String) {
        Log.d("SoundPod-WebView-JS", message)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebView() {
    // Hold pending requests in state
    val decipherRequests = remember { ConcurrentHashMap<String, CompletableDeferred<String>>() }

    AndroidView(
        modifier = Modifier.size(100.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                // Using a modern mobile User-Agent
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

                //Attach the explicitly defined bridge
                addJavascriptInterface(SoundPodJsBridge(decipherRequests), "SoundPodBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        Log.d("SoundPod-WebView", "Page finished: $url")

                        // Ensure cookies are flushed to storage
                        CookieManager.getInstance().flush()
                        val cookies = CookieManager.getInstance().getCookie(url)

                        //Safer JavaScript evaluation with proper null checks
                        val extractVisitorDataJs = """
                            (function() { 
                                const visitorData = window.yt?.config_?.VISITOR_DATA || 
                                       (window.ytcfg && ytcfg.get ? ytcfg.get('VISITOR_DATA') : null); 
                                SoundPodBridge.log('JS: extracted visitorData: ' + visitorData);
                                return visitorData;
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
                                            (function() {
                                                if (typeof decipherNParam === 'function') {
                                                    decipherNParam('$nParam', '$requestId');
                                                } else {
                                                    SoundPodBridge.log('JS: decipherNParam not ready for ' + '$nParam');
                                                    SoundPodBridge.onDecipherResult('$requestId', '$nParam');
                                                }
                                            })();
                                        """.trimIndent()

                                        view.post {
                                            view.evaluateJavascript(decipherInvokeJs, null)
                                        }

                                        val result = deferred.await()
                                        Log.d("SoundPod-WebView", "Decipher nParam: $nParam -> $result")
                                        result
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
                                        
                                        SoundPodBridge.log('JS: BotGuard challenge: ' + (challenge ? 'found' : 'NOT found'));
                                        
                                        if (challenge) {
                                            const result = await runBotGuard(challenge);
                                            if (result && result.botguardResponse) {
                                                SoundPodBridge.onPoTokenResult(result.botguardResponse);
                                            } else {
                                                SoundPodBridge.log('JS: runBotGuard returned no response');
                                            }
                                        }
                                    } catch (e) {
                                        SoundPodBridge.log('JS: BotGuard error: ' + e.message);
                                    }
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
        (function() {
            if (window.decipherFunction) {
                SoundPodBridge.log('JS: decipherFunction already exists');
                return;
            }
            
            function findDecipherFunction() {
                SoundPodBridge.log('JS: searching for decipher function...');
                try {
                    // Try to find it in common locations
                    if (window._yt_player) {
                         for (const key in window._yt_player) {
                             const val = window._yt_player[key];
                             if (typeof val === 'function' && val.length === 1) {
                                 const str = val.toString();
                                 if (str.includes('.split("")') && str.includes('.join("")')) {
                                     SoundPodBridge.log('JS: Found decipher in window._yt_player.' + key);
                                     return val;
                                 }
                             }
                         }
                    }

                    const keys = Object.keys(window);
                    for (const key of keys) {
                        if (key.startsWith('_yt_')) {
                            const val = window[key];
                            if (typeof val === 'object' && val !== null) {
                                for (const subKey in val) {
                                    try {
                                        const subVal = val[subKey];
                                        if (typeof subVal === 'function' && subVal.length === 1) {
                                            const str = subVal.toString();
                                            if (str.includes('.split("")') && str.includes('.join("")')) {
                                                SoundPodBridge.log('JS: Found potential decipher function in ' + key + '.' + subKey);
                                                return subVal;
                                            }
                                        }
                                    } catch(e) {}
                                }
                            }
                        }
                    }
                } catch(e) {
                    SoundPodBridge.log('JS: findDecipherFunction error: ' + e.message);
                }
                return null;
            }

            window.decipherFunction = findDecipherFunction();
            
            window.decipherNParam = function(n, requestId) {
                SoundPodBridge.log('JS: decipherNParam called for ' + n);
                let result = n; 
                if (!window.decipherFunction) {
                    window.decipherFunction = findDecipherFunction();
                }
                
                if (window.decipherFunction) {
                    try { 
                        result = window.decipherFunction(n); 
                        SoundPodBridge.log('JS: decipher success: ' + n + ' -> ' + result);
                    } catch(e) { 
                        SoundPodBridge.log('JS: decipher execution error: ' + e.message); 
                    }
                } else {
                    SoundPodBridge.log('JS: Decipher function not found even after retry');
                }
                SoundPodBridge.onDecipherResult(requestId, result);
            };
            
            SoundPodBridge.log('JS: injectDecipherScript initialization complete');
        })();
    """.trimIndent()
    webView.evaluateJavascript(script, null)
}
