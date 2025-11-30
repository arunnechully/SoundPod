package com.github.soundpod.ui.common

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.core.ui.ColorMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

val Context.updateDataStore by preferencesDataStore("update_prefs")

object UpdatePrefs {
    val AUTO_CHECK_ENABLED = booleanPreferencesKey("auto_check_enabled")
    val SHOW_UPDATE_ALERT = booleanPreferencesKey("show_update_alert")

    val THEME_KEY = stringPreferencesKey("theme_preference")

}

fun autoCheckEnabled(context: Context) =
    context.updateDataStore.data.map {
        it[UpdatePrefs.AUTO_CHECK_ENABLED] ?: true  // default ON
    }

fun showUpdateAlert(context: Context) =
    context.updateDataStore.data.map {
        it[UpdatePrefs.SHOW_UPDATE_ALERT] ?: true   // default ON
    }

suspend fun setAutoCheckEnabled(context: Context, value: Boolean) {
    context.updateDataStore.edit {
        it[UpdatePrefs.AUTO_CHECK_ENABLED] = value
    }
}

suspend fun setShowUpdateAlert(context: Context, value: Boolean) {
    context.updateDataStore.edit {
        it[UpdatePrefs.SHOW_UPDATE_ALERT] = value
    }
}

fun themePreference(context: Context) =
    context.updateDataStore.data.map {
        val value = it[UpdatePrefs.THEME_KEY] ?: ColorMode.System.name
        ColorMode.valueOf(value)
    }

suspend fun setThemePreference(context: Context, mode: ColorMode) {
    context.updateDataStore.edit {
        it[UpdatePrefs.THEME_KEY] = mode.name
    }
}

private val SEAMLESS_UPDATE_ENABLED = booleanPreferencesKey("seamless_update_enabled")

fun seamlessUpdateEnabled(context: Context): Flow<Boolean> = context.updateDataStore.data
    .map { preferences ->
        preferences[SEAMLESS_UPDATE_ENABLED] ?: false
    }
suspend fun setSeamlessUpdateEnabled(context: Context, enabled: Boolean) {
    context.updateDataStore.edit { preferences ->
        preferences[SEAMLESS_UPDATE_ENABLED] = enabled
    }
}

sealed class UpdateStatus {
    object Checking : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Available(val version: String, val downloadUrl: String, val size: Long) : UpdateStatus()
    data class Downloading(val progress: Float) : UpdateStatus()
    data class ReadyToInstall(val file: File) : UpdateStatus()
    data class DownloadedToPublic(val file: File) : UpdateStatus()
    object Installing : UpdateStatus()
    object Error : UpdateStatus()
}