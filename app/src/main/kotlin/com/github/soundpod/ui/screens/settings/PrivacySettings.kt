package com.github.soundpod.ui.screens.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.soundpod.R
import com.github.soundpod.ui.components.SettingsScreenLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Privacy(
    onBackClick: () -> Unit
) {
    SettingsScreenLayout(
        title =stringResource(id = R.string.privacy),
        onBackClick = onBackClick,
        content = {

        }
    )
}