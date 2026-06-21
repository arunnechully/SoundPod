package com.github.soundpod.service

import android.util.Log
import com.github.innertube.Innertube
import com.squareup.duktape.Duktape
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

object YouTubeDecipherer {
    private const val TAG = "SoundPod-Decipherer"
    private val scriptCache = AtomicReference<String?>(null)
    private val decipherFunctionName = AtomicReference<String?>(null)
    
    suspend fun decipher(n: String): String {
        val script = scriptCache.get()
        val funcName = decipherFunctionName.get()
        
        if (script == null || funcName == null) {
            Log.w(TAG, "Decipherer not initialized, returning original n")
            return n
        }
        
        return withContext(Dispatchers.Default) {
            try {
                Duktape.create().use { duktape ->
                    duktape.evaluate(script)
                    val result = duktape.evaluate("$funcName('$n')") as String
                    result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decipher n-parameter with Duktape", e)
                n
            }
        }
    }
    
    suspend fun initialize(jsUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing decipherer from $jsUrl")
                val fullUrl = if (jsUrl.startsWith("http")) jsUrl else "https://music.youtube.com$jsUrl"
                
                // Use Innertube.client to fetch the JS content
                val response = Innertube.client.get(fullUrl)
                val jsContent = response.bodyAsText()
                
                // Extract the n-parameter decipher function
                val name = extractDecipherFunctionName(jsContent)
                if (name != null) {
                    val functionBody = extractFunction(jsContent, name)
                    if (functionBody != null) {
                        scriptCache.set(functionBody)
                        decipherFunctionName.set(name)
                        Log.i(TAG, "Successfully initialized decipherer with function: $name")
                    } else {
                        Log.w(TAG, "Could not extract function body for $name")
                    }
                } else {
                    Log.w(TAG, "Could not find decipher function name in JS")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize decipherer", e)
            }
        }
    }
    
    private fun extractDecipherFunctionName(js: String): String? {
        // Pattern for finding the n-decipher function name in modern base.js
        // Updated regex to handle more variations
        val regex = Regex("""\.get\("n"\)\)&&\(b=([a-zA-Z0-9$]+)\(b\)""")
        return regex.find(js)?.groupValues?.get(1)
    }
    
    private fun extractFunction(js: String, name: String): String? {
        // Updated regex to be more robust
        val patterns = listOf(
            Regex("""var\s+${Regex.escape(name)}\s*=\s*function\s*\(([a-z]+)\)\{"""),
            Regex("""function\s+${Regex.escape(name)}\s*\(([a-z]+)\)\{""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(js) ?: continue
            val startIndex = match.range.first
            
            var braceCount = 0
            var foundFirstBrace = false
            for (i in match.range.last until js.length) {
                if (js[i] == '{') {
                    braceCount++
                    foundFirstBrace = true
                } else if (js[i] == '}') {
                    braceCount--
                }
                
                if (foundFirstBrace && braceCount == 0) {
                    return js.substring(startIndex, i + 1)
                }
            }
        }
        return null
    }
}
