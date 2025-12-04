package com.github.soundpod.viewmodels

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.soundpod.R
import com.github.soundpod.ui.common.newSearchLayoutEnabled // Import this
import com.github.soundpod.ui.common.setNewSearchLayoutEnabled // Import this
import com.github.soundpod.ui.navigation.SettingsDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Menu Structure
    private val _sections = MutableStateFlow<List<SettingsSection>>(emptyList())
    val sections = _sections.asStateFlow()

    // Search Toggle State
    private val _newSearchEnabled = MutableStateFlow(false)
    val newSearchEnabled = _newSearchEnabled.asStateFlow()

    init {
        loadSettings()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            // Observe the Flow from your DataStore helper
            newSearchLayoutEnabled(context).collect { isEnabled ->
                _newSearchEnabled.value = isEnabled
            }
        }
    }

    fun setNewSearchEnabled(enabled: Boolean) {
        // Update DataStore (The Flow above will automatically update the UI state)
        viewModelScope.launch {
            setNewSearchLayoutEnabled(context, enabled)
        }
    }

    private fun loadSettings() {
        val menuStructure = listOf(
            // ... (Keep your Section 1, 2, 3) ...
            SettingsSection(
                listOf(
                    SettingOption(title = R.string.appearance, icon = Icons.Default.ColorLens, screenId = SettingsDestinations.APPEARANCE),
                    SettingOption(title = R.string.player, icon = Icons.Default.PlayArrow, screenId = SettingsDestinations.PLAYER)
                )
            ),
            SettingsSection(
                listOf(
                    SettingOption(title = R.string.privacy, icon = Icons.Default.PrivacyTip, screenId = SettingsDestinations.PRIVACY)
                )
            ),
            SettingsSection(
                listOf(
                    SettingOption(title = R.string.backup_restore, icon = Icons.Default.Restore, screenId = SettingsDestinations.BACKUP),
                    SettingOption(title = R.string.database, icon = Icons.Default.Storage, screenId = SettingsDestinations.DATABASE)
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
                    // Ensure this ID matches what you use in NavHost
                    SettingOption(
                        title = R.string.experimental,
                        iconRes = R.drawable.experimental,
                        screenId = SettingsDestinations.EXPERIMENT
                    )
                )
            ),
            SettingsSection(
                listOf(
                    SettingOption(title = R.string.about, icon = Icons.Default.Info, screenId = SettingsDestinations.ABOUT)
                )
            )
        )
        _sections.value = menuStructure
    }
}