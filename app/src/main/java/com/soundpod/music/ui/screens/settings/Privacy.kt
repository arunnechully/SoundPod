package com.soundpod.music.ui.screens.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.soundpod.music.R
import com.soundpod.music.ui.components.settings.SettingsScreenLayout

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