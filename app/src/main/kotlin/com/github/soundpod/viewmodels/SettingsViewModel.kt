package com.github.soundpod.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.lifecycle.ViewModel
import com.github.soundpod.R
import com.github.soundpod.ui.navigation.SettingsDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {

    private val _sections = MutableStateFlow<List<SettingsSection>>(emptyList())
    val sections = _sections.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val menuStructure = listOf(
            // Section 1: Appearance & Player
            SettingsSection(
                listOf(
                    SettingOption(
                        title = R.string.appearance,
                        icon = Icons.Default.ColorLens,
                        screenId = SettingsDestinations.APPEARANCE
                    ),
                    SettingOption(
                        title = R.string.player,
                        icon = Icons.Default.PlayArrow,
                        screenId = SettingsDestinations.PLAYER
                    )
                )
            ),
            // Section 2: Privacy
            SettingsSection(
                listOf(
                    SettingOption(
                        title = R.string.privacy,
                        icon = Icons.Default.PrivacyTip,
                        screenId = SettingsDestinations.PRIVACY
                    )
                )
            ),
            // Section 3: Data
            SettingsSection(
                listOf(
                    SettingOption(
                        title = R.string.backup_restore,
                        icon = Icons.Default.Restore,
                        screenId = SettingsDestinations.BACKUP
                    ),
                    SettingOption(
                        title = R.string.database,
                        icon = Icons.Default.Storage,
                        screenId = SettingsDestinations.DATABASE
                    )
                )
            ),
            // Section 4: Advanced
            SettingsSection(
                listOf(
                    SettingOption(
                        title = R.string.more_settings,
                        iconRes = R.drawable.more_settings,
                        screenId = SettingsDestinations.MORE
                    ),
                    SettingOption(
                        title = R.string.experimental,
                        iconRes = R.drawable.experimental,
                        screenId = SettingsDestinations.EXPERIMENT
                    )
                )
            ),
            // Section 5: About
            SettingsSection(
                listOf(
                    SettingOption(
                        title = R.string.about,
                        icon = Icons.Default.Info,
                        screenId = SettingsDestinations.ABOUT
                    )
                )
            )
        )
        _sections.value = menuStructure
    }
}