package com.github.soundpod.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

const val coilDiskCacheMaxSizeKey = "coilDiskCacheMaxSize"
const val exoPlayerDiskCacheMaxSizeKey = "exoPlayerDiskCacheMaxSize"
const val isInvincibilityEnabledKey = "isInvincibilityEnabled"
const val songSortOrderKey = "songSortOrder"
const val songSortByKey = "songSortBy"
const val playlistSortOrderKey = "playlistSortOrder"
const val playlistSortByKey = "playlistSortBy"
const val albumSortOrderKey = "albumSortOrder"
const val albumSortByKey = "albumSortBy"
const val artistSortOrderKey = "artistSortOrder"
const val artistSortByKey = "artistSortBy"
const val trackLoopEnabledKey = "trackLoopEnabled"
const val queueLoopEnabledKey = "queueLoopEnabled"
const val skipSilenceKey = "skipSilence"
const val volumeNormalizationKey = "volumeNormalization"
const val resumePlaybackWhenDeviceConnectedKey = "resumePlaybackWhenDeviceConnected"
const val persistentQueueKey = "persistentQueue"
const val isShowingSynchronizedLyricsKey = "isShowingSynchronizedLyrics"
const val isShowingThumbnailInLockscreenKey = "isShowingThumbnailInLockscreen"
const val homeScreenTabIndexKey = "homeScreenTabIndex"
const val searchResultScreenTabIndexKey = "searchResultScreenTabIndex"
const val artistScreenTabIndexKey = "artistScreenTabIndex"
const val pauseSearchHistoryKey = "pauseSearchHistory"

const val pauseSongCacheKey = "pauseSongCache"
const val quickPicksSourceKey = "quickPicksSource"

const val appTheme = "appTheme"
const val accentColorSource = "accentColorSource"
const val progressBarStyle = "progressBarStyle"
const val navigationLabelsVisibilityKey = "navigationLabelsVisibility"
const val listGesturesEnabledKey = "listGesturesEnabled"
const val playerGesturesEnabledKey = "songGesturesEnabled"
const val miniplayerGesturesEnabledKey = "miniplayerGesturesEnabled"

inline fun <reified T : Enum<T>> SharedPreferences.getEnum(
    key: String,
    defaultValue: T
): T =
    getString(key, null)?.let {
        try {
            enumValueOf<T>(it)
        } catch (_: IllegalArgumentException) {
            null
        }
    } ?: defaultValue

inline fun <reified T : Enum<T>> SharedPreferences.Editor.putEnum(
    key: String,
    value: T
): SharedPreferences.Editor =
    putString(key, value.name)

val Context.preferences: SharedPreferences
    get() = getSharedPreferences("preferences", Context.MODE_PRIVATE)

@Composable
fun rememberPreference(key: String, defaultValue: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val preferences = context.preferences
    val state = remember { mutableStateOf(preferences.getBoolean(key, defaultValue)) }

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, changedKey ->
            if (changedKey == key) {
                state.value = sharedPrefs.getBoolean(key, defaultValue)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (state.value != preferences.getBoolean(key, defaultValue)) {
            preferences.edit { putBoolean(key, state.value) }
        }
    }

    return state
}

@Composable
fun rememberPreference(key: String, defaultValue: Int): MutableState<Int> {
    val context = LocalContext.current
    val preferences = context.preferences
    val state = remember { mutableStateOf(preferences.getInt(key, defaultValue)) }

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, changedKey ->
            if (changedKey == key) {
                state.value = sharedPrefs.getInt(key, defaultValue)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (state.value != preferences.getInt(key, defaultValue)) {
            preferences.edit { putInt(key, state.value) }
        }
    }

    return state
}

@Composable
fun rememberPreference(key: String, defaultValue: String): MutableState<String> {
    val context = LocalContext.current
    val preferences = context.preferences
    val state = remember { mutableStateOf(preferences.getString(key, null) ?: defaultValue) }

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, changedKey ->
            if (changedKey == key) {
                state.value = sharedPrefs.getString(key, null) ?: defaultValue
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (state.value != (preferences.getString(key, null) ?: defaultValue)) {
            preferences.edit { putString(key, state.value) }
        }
    }

    return state
}

@Composable
inline fun <reified T : Enum<T>> rememberPreference(key: String, defaultValue: T): MutableState<T> {
    val context = LocalContext.current
    val preferences = context.preferences
    val state = remember { mutableStateOf(preferences.getEnum(key, defaultValue)) }

    DisposableEffect(key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, changedKey ->
            if (changedKey == key) {
                state.value = sharedPrefs.getEnum(key, defaultValue)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(state.value) {
        if (state.value != preferences.getEnum(key, defaultValue)) {
            preferences.edit { putEnum(key, state.value) }
        }
    }

    return state
}
