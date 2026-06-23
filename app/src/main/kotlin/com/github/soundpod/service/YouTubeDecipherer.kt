package com.github.soundpod.service

import android.util.Log
import com.github.innertube.Innertube
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context
import java.util.concurrent.atomic.AtomicReference

object YouTubeDecipherer {
    private const val TAG = "SoundPod-Decipherer"
    private val nScriptCache = AtomicReference<String?>(null)
    private val nFunctionName = AtomicReference<String?>(null)
    private val sigScriptCache = AtomicReference<String?>(null)
    private val sigFunctionName = AtomicReference<String?>(null)
    
    suspend fun decipher(n: String): String {
        val script = nScriptCache.get()
        val funcName = nFunctionName.get()
        
        if (script == null || funcName == null) {
            Log.w(TAG, "N-Decipherer not initialized, returning original n")
            return n
        }
        
        return withContext(Dispatchers.Default) {
            try {
                val rhino = Context.enter()
                rhino.optimizationLevel = -1
                try {
                    val scope = rhino.initSafeStandardObjects()
                    rhino.evaluateString(scope, script, "JavaScript", 1, null)
                    val result = rhino.evaluateString(scope, "$funcName('$n')", "JavaScript", 1, null)
                    val finalResult = Context.toString(result)
                    Log.d(TAG, "Successfully deciphered n: $n -> $finalResult")
                    finalResult
                } finally {
                    Context.exit()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decipher n-parameter with Rhino", e)
                n
            }
        }
    }

    suspend fun signatureDecipher(s: String): String {
        val script = sigScriptCache.get()
        val funcName = sigFunctionName.get()
        
        if (script == null || funcName == null) {
            Log.w(TAG, "Sig-Decipherer not initialized, returning original s")
            return s
        }
        
        return withContext(Dispatchers.Default) {
            try {
                val rhino = Context.enter()
                rhino.optimizationLevel = -1
                try {
                    val scope = rhino.initSafeStandardObjects()
                    rhino.evaluateString(scope, script, "JavaScript", 1, null)
                    val result = rhino.evaluateString(scope, "$funcName('$s')", "JavaScript", 1, null)
                    Context.toString(result)
                } finally {
                    Context.exit()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decipher signature with Rhino", e)
                s
            }
        }
    }
    
    suspend fun initialize(jsUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing decipherer from $jsUrl")
                val fullUrl = if (jsUrl.startsWith("http")) jsUrl else "https://music.youtube.com$jsUrl"
                
                val response = Innertube.client.get(fullUrl)
                val jsContent = response.bodyAsText()
                
                // 1. Handle N-Parameter
                val nName = extractDecipherFunctionName(jsContent)
                if (nName != null) {
                    val nFunctionBody = extractFunction(jsContent, nName)
                    if (nFunctionBody != null) {
                        nScriptCache.set(nFunctionBody)
                        nFunctionName.set(nName)
                        Log.i(TAG, "Successfully initialized n-decipherer: $nName")
                    }
                }

                // 2. Handle Signature
                val sigName = extractSignatureFunctionName(jsContent)
                if (sigName != null) {
                    val sigFunctionBody = extractSignatureFunctionAndHelpers(jsContent, sigName)
                    if (sigFunctionBody != null) {
                        sigScriptCache.set(sigFunctionBody)
                        sigFunctionName.set(sigName)
                        Log.i(TAG, "Successfully initialized sig-decipherer: $sigName")
                    }
                }

                if (nFunctionName.get() != null || sigFunctionName.get() != null) {
                    YouTubeSessionManager.updateSession(
                        decipher = ::decipher,
                        signatureDecipher = ::signatureDecipher
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize decipherer", e)
            }
        }
    }
    
    private fun extractDecipherFunctionName(js: String): String? {
        val patterns = listOf(
            Regex("""\.get\("n"\)\)&&\(b=([a-zA-Z0-9$]+)\(b\)"""),
            Regex("""([a-zA-Z0-9$]+)\s*=\s*function\([a-z]\)\{var\s+[a-z]=\[.*];[a-z]\.set\("n",[a-z]\)"""),
            Regex("""([a-zA-Z0-9$]+)=function\([a-z]\)\{var\s+[a-z]=\[.*];[a-z]\.set\("n",[a-z]\)""")
        )
        
        for (pattern in patterns) {
            pattern.find(js)?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    private fun extractSignatureFunctionName(js: String): String? {
        val patterns = listOf(
            Regex("""\b[cs]\s*&&\s*[ad]\.set\([^,]+\s*,\s*encodeURIComponent\s*\(\s*([a-zA-Z0-9$]+)\s*\("""),
            Regex("""\.sig\|\|([a-zA-Z0-9$]+)\("""),
            Regex("""([a-zA-Z0-9$]+)\s*=\s*function\s*\(\s*a\s*\)\s*\{\s*a\s*=\s*a\.split\(\s*""\s*\)""")
        )
        for (pattern in patterns) {
            pattern.find(js)?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    private fun extractSignatureFunctionAndHelpers(js: String, name: String): String? {
        val functionBody = extractFunction(js, name) ?: return null
        
        // Find the helper object name used in the function (e.g. "abc.de(a, 2)" -> "abc")
        val helperNameRegex = Regex(""";([a-zA-Z0-9$]+)\.[a-zA-Z0-9$]+\(""")
        val helperName = helperNameRegex.find(functionBody)?.groupValues?.get(1) ?: return functionBody
        
        val helperObject = extractObject(js, helperName) ?: ""
        return helperObject + "\n" + functionBody
    }

    private fun extractObject(js: String, name: String): String? {
        val pattern = Regex("""var\s+${Regex.escape(name)}\s*=\s*\{""")
        val match = pattern.find(js) ?: return null
        val startIndex = match.range.first
        
        var braceCount = 0
        var foundFirstBrace = false
        for (i in startIndex until js.length) {
            if (js[i] == '{') {
                braceCount++
                foundFirstBrace = true
            } else if (js[i] == '}') {
                braceCount--
            }
            
            if (foundFirstBrace && braceCount == 0) {
                return "var $name =" + js.substring(startIndex + "var $name =".length - 4, i + 1) + ";"
            }
        }
        return null
    }

    private fun extractFunction(js: String, name: String): String? {
        val patterns = listOf(
            Regex("""var\s+${Regex.escape(name)}\s*=\s*function\s*\(([a-z,]+)\)\{"""),
            Regex("""function\s+${Regex.escape(name)}\s*\(([a-z,]+)\)\{"""),
            Regex("""${Regex.escape(name)}\s*=\s*function\s*\(([a-z,]+)\)\{""")
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
