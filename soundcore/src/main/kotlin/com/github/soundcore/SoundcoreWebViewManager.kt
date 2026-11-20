package com.github.soundcore

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient

@SuppressLint("SetJavaScriptEnabled", "StaticFieldLeak")
object SoundcoreWebViewManager {
    private var webView: WebView? = null

    fun initialize(context: Context) {
        if (webView != null) return

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if (webView != null) return@post

            webView = WebView(context.applicationContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Mobile Safari/537.36"

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        Log.d("SoundPod-SoundcoreWebView", "Page finished: $url")
                        CookieManager.getInstance().flush()
                    }
                }

                loadUrl("https://www.jiosaavn.com")
            }
        }
    }
}
