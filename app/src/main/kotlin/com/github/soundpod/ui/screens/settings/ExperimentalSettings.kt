package com.github.soundpod.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentSettings(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val isNewSearchEnabled by viewModel.newSearchEnabled.collectAsState()

    BackHandler(onBack = onBackClick)

    SettingsScreenLayout(
        title = stringResource(id = R.string.experimental),
        onBackClick = onBackClick,
        content = {

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
            }
        }
    )
}