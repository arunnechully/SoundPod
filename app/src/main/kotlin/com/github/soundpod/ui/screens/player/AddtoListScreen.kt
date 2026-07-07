package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.soundpod.R
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout

@Composable
fun AddToListScreen(
    onBackClick: () -> Unit
) {
    SettingsScreenLayout(
        title = {
            Text(
                text = stringResource(R.string.add_to_playlist)
            )
        },
        scrollable = false,
        onBackClick = onBackClick,
        horizontalPadding = 0.dp
    ) {
        SettingsCard(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Content for adding to list goes here
        }
    }
}