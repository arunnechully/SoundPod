package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.soundpod.R
import com.github.soundpod.service.YouTubeSessionManager
import com.github.soundpod.ui.components.SettingsCard

@Composable
fun YouTubeSettingsContent() {
    val isSessionReady by YouTubeSessionManager.isSessionReady.collectAsState()
    val needsConsent by YouTubeSessionManager.needsConsent.collectAsState()

    Column {
        SettingsCard {
            SettingRow(
                title = if (isSessionReady) stringResource(R.string.consent_granted) else stringResource(R.string.consent_required),
                onClick = {}
            )
            
            SettingRow(
                title = stringResource(R.string.renew_consent),
                onClick = { YouTubeSessionManager.setNeedsConsent(true) }
            )
        }
        
        Text(
            text = "If the homepage is not loading or songs are spinning endlessly, use 'Renew Consent' to manually refresh your YouTube session. This is common for users in the EU.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
