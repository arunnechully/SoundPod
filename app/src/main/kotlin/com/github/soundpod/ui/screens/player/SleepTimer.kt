package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.utils.formatAsDuration

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimer(
    sleepTimerMillisLeft: Long?,
    onDismiss: () -> Unit
) {
    val binder = LocalPlayerServiceBinder.current ?: return

    if (sleepTimerMillisLeft != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        binder.cancelSleepTimer()
                        onDismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.stop))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.stop_sleep_timer_dialog))
            },
            text = {
                sleepTimerMillisLeft.let {
                    FlowColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 8.dp,
                            alignment = Alignment.CenterVertically
                        ),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 8.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(126.sp.value.dp)
                                .border(
                                    width = 4.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatAsDuration(it),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Button(
                            onClick = {
                                binder.startSleepTimer(it + 60 * 1000L)
                                onDismiss()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(text = "+1:00")
                        }
                    }
                }
            }
        )
    } else {
        var amount by remember { mutableIntStateOf(1) }

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        binder.startSleepTimer(amount * 10 * 60 * 1000L)
                        onDismiss()
                    },
                    enabled = amount > 0
                ) {
                    Text(text = stringResource(id = R.string.set))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.set_sleep_timer))
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 16.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { amount-- },
                        enabled = amount > 1
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            contentDescription = null
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${amount / 6}h ${(amount % 6) * 10}m",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { amount++ },
                        enabled = amount < 60
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }
}