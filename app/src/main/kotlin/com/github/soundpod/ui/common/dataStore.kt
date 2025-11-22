package com.github.soundpod.ui.common

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.updateDataStore by preferencesDataStore("update_prefs")

object UpdatePrefs {
    val AUTO_CHECK_ENABLED = booleanPreferencesKey("auto_check_enabled")
    val SHOW_UPDATE_ALERT = booleanPreferencesKey("show_update_alert")
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
