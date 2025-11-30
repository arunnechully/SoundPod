package com.github.soundpod.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.soundpod.R
import com.github.soundpod.ui.components.SettingsScreenLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettings(
    onBackClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    SettingsScreenLayout(
        title =stringResource(id = R.string.privacy),
        onBackClick = onBackClick,
        content = {

        }
    )
}