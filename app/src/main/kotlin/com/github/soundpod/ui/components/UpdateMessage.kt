package com.github.soundpod.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import com.github.api.formatFileSize
import com.github.soundpod.ui.common.UpdateStatus

@Composable
fun UpdateMessage(
    status: UpdateStatus,
    onUpdateClick: (String) -> Unit
) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    var ignoreFutureUpdates by remember { mutableStateOf(false) }

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
                Text("Update Now")
            }
        } else {
            val statusText = when (status) {
                is UpdateStatus.UpToDate -> "The latest version is already installed"
                else -> ""
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
            onDismissRequest = {},
            title = {
                Text(text = "New Update Available")
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // TODO: Implement Changelog fetching here later
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "• Bug fixes and improvements\n• Performance enhancements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // "Remove for future" Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = ignoreFutureUpdates,
                            onCheckedChange = { ignoreFutureUpdates = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Skip this version", // "Remove for future" logic
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: If ignoreFutureUpdates is true, save preference to DataStore
                        if (!ignoreFutureUpdates) {
                            onUpdateClick(status.downloadUrl)
                        }
                    }
                ) {
                    Text("Download & Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            }
        )
    }
}