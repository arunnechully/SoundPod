@file:Suppress("AssignedValueIsNeverRead")

package com.github.soundpod.ui.github

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.api.formatFileSize
import com.github.soundpod.R
import com.github.soundpod.ui.common.UpdateStatus
import com.github.soundpod.ui.components.LoadingAnimation
import java.io.File

@Composable
fun UpdateMessage(
    status: UpdateStatus,
    onUpdateClick: (String) -> Unit,
    onInstallClick: (File) -> Unit,
) {
    var showUpdateDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (status is UpdateStatus.Checking) {
            LoadingAnimation(
                modifier = Modifier.size(50.dp)
            )
        } else if (status is UpdateStatus.Available) {
            Button(
                onClick = { showUpdateDialog = true },
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(40.dp)
            ) {
                Text(stringResource(id = R.string.update_now))
            }
        } else if (status is UpdateStatus.Downloading) {
            DownloadProgressBar(status)
        }
        else if (status is UpdateStatus.ReadyToInstall) {
            Button(
                onClick = {onInstallClick(status.file) },
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(40.dp)
            ) {
                Text(stringResource(id = R.string.install))
            }
        } else {
            val statusText = when (status) {
                is UpdateStatus.UpToDate ->
                    stringResource(id = R.string.latest_version_installed)

                is UpdateStatus.Installing ->
                    stringResource(id = R.string.installing_update)

                is UpdateStatus.DownloadedToPublic ->
                    stringResource(id = R.string.update_manually)

                is UpdateStatus.Error ->
                    stringResource(id = R.string.update_failed)
            }


            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showUpdateDialog && status is UpdateStatus.Available) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog  = false},
            title = {
                Text(text = stringResource(id = R.string.updates_available))
            },
            text = {
                Column {
                    // Version Name
                    Text(
                        text = "Version: ${status.version}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // File Size
                    Text(
                        text = "Size: ${formatFileSize(status.size)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {onUpdateClick(status.downloadUrl)}
                ) {
                    Text(stringResource(id = R.string.download_and_update))
                }
            },
            dismissButton = {
                TextButton(onClick = {showUpdateDialog = false}) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}