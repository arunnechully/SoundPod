package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.viewmodels.SettingsViewModel

@Composable
fun ExperimentSettingsContent(
    viewModel: SettingsViewModel = viewModel()
) {
    val isNewSearchEnabled by viewModel.newSearchEnabled.collectAsState()
    val isLoginExperimentalEnabled by viewModel.loginExperimentalEnabled.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = stringResource(id = R.string.login_experimental_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.login_experimental_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setLoginExperimentalEnabled(true)
                        showDialog = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Column {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsCard {
            SwitchSetting(
                icon = IconSource.Vector(Icons.Default.Search),
                title = stringResource(id = R.string.new_search_screen),
                description = stringResource(id = R.string.new_search_screen_description),
                switchState = isNewSearchEnabled,
                onSwitchChange = { isChecked ->
                    viewModel.setNewSearchEnabled(isChecked)
                },
            )

            SwitchSetting(
                icon = IconSource.Vector(Icons.Default.VpnKey),
                title = stringResource(id = R.string.login_experimental),
                description = stringResource(id = R.string.login_experimental_description),
                switchState = isLoginExperimentalEnabled,
                onSwitchChange = { isChecked ->
                    if (isChecked) {
                        showDialog = true
                    } else {
                        viewModel.setLoginExperimentalEnabled(false)
                    }
                },
            )
        }
    }
}
