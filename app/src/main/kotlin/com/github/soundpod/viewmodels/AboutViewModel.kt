@file:Suppress("KotlinConstantConditions")
package com.github.soundpod.viewmodels

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.api.GitHub
import com.github.soundpod.BuildConfig
import com.github.soundpod.R
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val isRecordingLogs = _isRecordingLogs.asStateFlow()
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

    fun toggleLogRecording() {
        if (_isRecordingLogs.value) {
            shareBugReport()
            _isRecordingLogs.value = false
        } else {
            recordingStartTime = System.currentTimeMillis()
            _isRecordingLogs.value = true
            Toast.makeText(context, context.getString(R.string.bug_report_started), Toast.LENGTH_LONG).show()
        }
    }

    private fun shareBugReport() {
        viewModelScope.launch(Dispatchers.IO) {
            val reportFile = File(context.externalCacheDir, "bug_report.txt")
            try {
                FileOutputStream(reportFile).use { output ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    output.write("SoundPod Bug Report\n".toByteArray())
                    output.write("===================\n".toByteArray())
                    output.write("Report Generated: ${sdf.format(Date())}\n".toByteArray())
                    output.write("Session Started: ${sdf.format(Date(recordingStartTime))}\n".toByteArray())
                    output.write("App Version: $currentVersion (${BuildConfig.VERSION_CODE})\n".toByteArray())
                    output.write("Flavor: ${BuildConfig.FLAVOR}\n".toByteArray())
                    output.write("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n".toByteArray())
                    output.write("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n".toByteArray())
                    
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    val memoryInfo = android.app.ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memoryInfo)
                    output.write("Available Memory: ${memoryInfo.availMem / 1024 / 1024} MB\n".toByteArray())
                    
                    output.write("--------------------------------\n".toByteArray())
                    output.write("LOGS (Filtered by PID):\n".toByteArray())
                    output.write("--------------------------------\n\n".toByteArray())

                    val pid = android.os.Process.myPid()
                    // Filter by PID to remove system bloat. Only supported on N+ for logcat --pid
                    val command = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        "logcat -d --pid=$pid"
                    } else {
                        "logcat -d"
                    }
                    
                    val process = Runtime.getRuntime().exec(command)
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            // Simple timestamp check to reduce old logs if logcat wasn't cleared
                            // We don't do complex parsing, just dump the PID-filtered logs
                            output.write("$line\n".toByteArray())
                        }
                    }
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", reportFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "SoundPod Bug Report ${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share Bug Report")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                saveBugReportToDownloads(reportFile)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.bug_report_failed), Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun saveBugReportToDownloads(reportFile: File) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "SoundPod_BugReport_${System.currentTimeMillis()}.txt")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/SoundPod")
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val uri = resolver.insert(collection, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    reportFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "Bug report saved to Downloads/SoundPod", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    companion object {
        private val _isRecordingLogs = MutableStateFlow(false)
        private var recordingStartTime: Long = 0
    }
}