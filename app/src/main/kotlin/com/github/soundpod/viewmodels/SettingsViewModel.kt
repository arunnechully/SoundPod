package com.github.soundpod.viewmodels

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.soundpod.R
import com.github.soundpod.ui.common.newSearchLayoutEnabled
import com.github.soundpod.ui.common.setNewSearchLayoutEnabled
import com.github.soundpod.ui.common.loginExperimentalEnabled
import com.github.soundpod.ui.common.setLoginExperimentalEnabled
import com.github.soundpod.ui.navigation.SettingsDestinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingOption(
    @param:StringRes val title: Int,
    val icon: ImageVector? = null,
    @get:DrawableRes val iconRes: Int? = null,
    val screenId: String
)

data class SettingsSection(
    val options: List<SettingOption>
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _sections = MutableStateFlow<List<SettingsSection>>(emptyList())
    val sections = _sections.asStateFlow()

    private val _newSearchEnabled = MutableStateFlow(false)
    val newSearchEnabled = _newSearchEnabled.asStateFlow()

    private val _loginExperimentalEnabled = MutableStateFlow(false)
    val loginExperimentalEnabled = _loginExperimentalEnabled.asStateFlow()

    init {
        loadSettings()
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            newSearchLayoutEnabled(getApplication()).collect { isEnabled ->
                _newSearchEnabled.value = isEnabled
            }
        }
        viewModelScope.launch {
            loginExperimentalEnabled(getApplication()).collect { isEnabled ->
                _loginExperimentalEnabled.value = isEnabled
                loadSettings()
            }
        }
    }

    fun setNewSearchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setNewSearchLayoutEnabled(getApplication(), enabled)
        }
    }

    fun setLoginExperimentalEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setLoginExperimentalEnabled(getApplication(), enabled)
        }
    }

    private fun loadSettings() {
        val menuStructure = mutableListOf<SettingsSection>()

        if (_loginExperimentalEnabled.value) {
            menuStructure.add(
                SettingsSection(
                    listOf(
                        SettingOption(title = R.string.account, icon = Icons.Default.Person, screenId = SettingsDestinations.ACCOUNT)
                    )
                )
            )
        }

        menuStructure.addAll(
            listOf(
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
                SettingsSection(
                    listOf(
                        SettingOption(title = R.string.about, icon = Icons.Default.Info, screenId = SettingsDestinations.ABOUT)
                    )
                )
            )
        )
        _sections.value = menuStructure
    }
}