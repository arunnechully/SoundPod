package com.github.soundpod.utils

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.*
import org.json.JSONObject

@Suppress("SpellCheckingInspection")
object YouTubeScraper {

    @SuppressLint("SetJavaScriptEnabled")
    fun setupScraperWebView(
        webView: WebView,
        onTokensScraped: (visitorData: String, poToken: String?) -> Unit
    ) {
        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"

            // Add the JavaScript Bridge
            addJavascriptInterface(object {
                @JavascriptInterface
                fun sendTokens(visitorData: String, poToken: String?) {
                    Log.d(
                        "SoundPodApp",
                        "INTERCEPTED: VisitorData=${visitorData.take(20)}..., POToken=${
                            poToken?.take(20)
                        }..."
                    )
                    onTokensScraped(visitorData, poToken)
                }
            }, "AndroidBridge")
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(cm: ConsoleMessage?) = true.also {
                Log.d("SoundPod-JS-Console", "${cm?.message()}")
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(
                    "SoundPodApp",
                    "WebView finished. Injecting Interceptor and Search Trigger..."
                )

                val interceptJs = """
    (function() {
        //Hook into fetch
        var originalFetch = window.fetch;
        window.fetch = function() {
            return originalFetch.apply(this, arguments).then(function(response) {
                if (response.url.includes('/youtubei/v1/')) {
                    response.clone().json().then(function(data) {
                        // Look for PO_TOKEN in serviceTrackingParams
                        var poToken = null;
                        try {
                            if (data.responseContext && data.responseContext.serviceTrackingParams) {
                                var stp = data.responseContext.serviceTrackingParams.find(p => p.service === 'GFX');
                                if (stp && stp.params) {
                                    poToken = stp.params.find(p => p.key === 'poToken').value;
                                }
                            }
                        } catch(e) {}
                        
                        var visitorData = window.ytcfg ? window.ytcfg.get('VISITOR_DATA') : "";
                        if (visitorData) {
                            AndroidBridge.sendTokens(visitorData, poToken || "");
                        }
                    });
                }
                return response;
            });
        };

        //Simulate real user behavior
        function simulateHuman() {
            var input = document.querySelector('input[type="text"]');
            if (input) {
                input.focus();
                input.value = 'I want to break free';
                input.dispatchEvent(new Event('input', { bubbles: true }));
                
                var button = document.querySelector('button[aria-label="Search"]');
                if (button) {
                    button.click();
                } else {
                    // Fallback: try pressing Enter if button isn't found
                    var event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true });
                    input.dispatchEvent(event);
                }
            } else {
                // Keep trying until search bar exists
                setTimeout(simulateHuman, 1000);
            }
        }
        
        // Give the page a moment to stabilize then "be human"
        setTimeout(simulateHuman, 2000);
    })();
""".trimIndent()
                view?.evaluateJavascript(interceptJs, null)
            }
        }
        webView.loadUrl("https://music.youtube.com")
    }
}