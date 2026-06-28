package com.github.soundpod.viewmodels

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import com.github.soundpod.R
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

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val menuStructure = mutableListOf<SettingsSection>()

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