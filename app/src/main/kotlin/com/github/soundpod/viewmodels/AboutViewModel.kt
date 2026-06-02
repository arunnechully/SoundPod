@file:Suppress("KotlinConstantConditions")
package com.github.soundpod.viewmodels

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.api.GitHub
import com.github.soundpod.BuildConfig
import com.github.soundpod.github.checkForUpdates
import com.github.soundpod.github.downloadAndInstall
import com.github.soundpod.github.installApkInternal
import com.github.soundpod.ui.common.UpdateStatus
import com.github.soundpod.ui.common.includePrerelease
import com.github.soundpod.ui.common.seamlessUpdateEnabled
import com.github.soundpod.ui.common.setIncludePrerelease
import com.github.soundpod.ui.common.setSeamlessUpdateEnabled
import com.github.soundpod.ui.common.setShowUpdateAlert
import com.github.soundpod.ui.common.showUpdateAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    // --- State ---
    
    val currentVersion: String = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            ).versionName
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Checking)
    val updateStatus = _updateStatus.asStateFlow()
    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog = _showPermissionDialog.asStateFlow()
    val seamlessUpdateEnabled: StateFlow<Boolean> = seamlessUpdateEnabled(getApplication())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showAlertEnabled: StateFlow<Boolean> = showUpdateAlert(getApplication())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val includePrerelease: StateFlow<Boolean> = includePrerelease(getApplication())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        if (BuildConfig.ENABLE_UPDATER) {
            viewModelScope.launch(Dispatchers.IO) {
                combine(
                    this@AboutViewModel.seamlessUpdateEnabled,
                    this@AboutViewModel.includePrerelease
                ) { isSeamless, includePre ->
                    isSeamless to includePre
                }.collect { (isSeamless, includePre) ->
                    checkForUpdates(getApplication(), currentVersion, isSeamless, includePre) {
                        _updateStatus.value = it
                    }
                }
            }
        }
    }

    fun onResume(hasInstallPermission: Boolean) {
        val isSeamlessEnabled = seamlessUpdateEnabled.value
        
        if (_showPermissionDialog.value && hasInstallPermission) {
            _showPermissionDialog.value = false
            setSeamlessEnabled(true)
            checkUpdates()
        }

        if (isSeamlessEnabled && !hasInstallPermission) {
            setSeamlessEnabled(false)
        }
    }

    fun toggleSeamlessUpdate(isChecked: Boolean, hasInstallPermission: Boolean) {
        if (isChecked) {
            if (!hasInstallPermission) {
                _showPermissionDialog.value = true
            } else {
                setSeamlessEnabled(true)
            }
        } else {
            setSeamlessEnabled(false)
        }
    }

    fun toggleUpdateAlert(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            setShowUpdateAlert(context, enabled)
        }
    }

    fun toggleIncludePrerelease(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            setIncludePrerelease(context, enabled)
        }
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
    }

    fun downloadUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            val includePre = includePrerelease.value
            val exactApkUrl = GitHub.getLatestReleaseApkUrl(BuildConfig.FLAVOR, includePre)
            val isSeamless = seamlessUpdateEnabled.value

            if (exactApkUrl != null) {
                downloadAndInstall(
                    context = context,
                    urlString = exactApkUrl,
                    isSeamless = isSeamless,
                    onProgress = { _updateStatus.value = UpdateStatus.Downloading(it) },
                    onFinished = { file ->
                        _updateStatus.value = if (isSeamless) {
                            UpdateStatus.ReadyToInstall(file)
                        } else {
                            UpdateStatus.DownloadedToPublic(file)
                        }
                    },
                    onError = { _updateStatus.value = UpdateStatus.Error }
                )
            } else {
                _updateStatus.value = UpdateStatus.Error
            }
        }
    }
    fun installUpdate(file: File) {
        _updateStatus.value = UpdateStatus.Installing
        viewModelScope.launch {
            delay(1500)
            installApkInternal(context, file)
        }
    }
    private fun setSeamlessEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            setSeamlessUpdateEnabled(context, enabled)
        }
    }
    private fun checkUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            checkForUpdates(context, currentVersion, seamlessUpdateEnabled.value, includePrerelease.value) {
                _updateStatus.value = it
            }
        }
    }
}