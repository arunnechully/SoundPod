package com.github.soundpod.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.soundpod.appContext
import kotlinx.serialization.json.Json

object ScreenCache {
    val preferences: SharedPreferences = appContext.getSharedPreferences("screen_cache", Context.MODE_PRIVATE)
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    val size: Long
        get() = try {
            java.io.File(appContext.applicationInfo.dataDir, "shared_prefs/screen_cache.xml").length()
        } catch (_: Exception) {
            0L
        }

    inline fun <reified T> save(key: String, data: T) {
        try {
            val serialized = json.encodeToString(data)
            preferences.edit {
                putString(key, serialized)
                putLong("${key}_time", System.currentTimeMillis())
            }
        } catch (_: Exception) {
        }
    }

    inline fun <reified T> load(key: String): T? {
        val serialized = preferences.getString(key, null) ?: return null
        return try {
            json.decodeFromString<T>(serialized)
        } catch (_: Exception) {
            null
        }
    }

    fun getTimestamp(key: String): Long {
        return preferences.getLong("${key}_time", 0)
    }
    
    fun isExpired(key: String, expirationTime: Long): Boolean {
        return System.currentTimeMillis() - getTimestamp(key) > expirationTime
    }

    fun remove(key: String) {
        preferences.edit {
            remove(key)
            remove("${key}_time")
        }
    }
}
