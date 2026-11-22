package com.github.soundpod.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.innertube.Innertube

@Suppress("unused")
private class SoundPodJsBridge {
    @JavascriptInterface
    fun log(message: String) {
        Log.d("SoundPod-WebViewManager-JS", message)
    }
}

@SuppressLint("SetJavaScriptEnabled", "StaticFieldLeak")
object YouTubeWebViewManager {
    private var webView: WebView? = null

    fun initialize(context: Context) {
        if (webView != null) return

        // Must run on main thread
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if (webView != null) return@post

            webView = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

                addJavascriptInterface(SoundPodJsBridge(), "SoundPodBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        Log.d("SoundPod-WebViewManager", "Page finished: $url")

                        CookieManager.getInstance().flush()
                        val cookies = CookieManager.getInstance().getCookie(url)

                        val extractSessionDataJs = """
                            (function() { 
                                const config = window.yt?.config_ || (window.ytcfg && ytcfg.getAll ? ytcfg.getAll() : {});
                                const visitorData = config.VISITOR_DATA || (window.ytcfg && ytcfg.get ? ytcfg.get('VISITOR_DATA') : null);
                                const apiKey = config.INNERTUBE_API_KEY || (window.ytcfg && ytcfg.get ? ytcfg.get('INNERTUBE_API_KEY') : null);
                                
                                SoundPodBridge.log('JS: extracted visitorData: ' + visitorData);
                                return JSON.stringify({
                                    visitorData: visitorData,
                                    apiKey: apiKey
                                });
                            })();
                        """.trimIndent()

                        view.evaluateJavascript(extractSessionDataJs) { result ->
                            try {
                                val json = org.json.JSONObject(result.replace("\\\"", "\"").trim('"'))
                                val cleanVisitorData = json.optString("visitorData").takeIf { it != "null" }
                                val apiKey = json.optString("apiKey").takeIf { it != "null" }

                                if (!cleanVisitorData.isNullOrBlank()) {
                                    Log.d("SoundPod-WebViewManager", "Extracted VisitorData: $cleanVisitorData")
                                    YouTubeSessionManager.updateSession(
                                        visitorData = cleanVisitorData,
                                        cookies = cookies
                                    )
                                }

                                if (!apiKey.isNullOrBlank()) {
                                    Innertube.apiKey = apiKey
                                }
                            } catch (e: Exception) {
                                Log.e("SoundPod-WebViewManager", "Failed to parse session data", e)
                            }
                        }
                    }
                }

                loadUrl("https://music.youtube.com")
            }
        }
    }
}
