package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.enums.AccentColorSource
import com.github.soundpod.enums.AppThemeColor
import com.github.soundpod.enums.ProgressBar
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.utils.accentColorSource
import com.github.soundpod.utils.appTheme
import com.github.soundpod.utils.progressBarStyle
import com.github.soundpod.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Appearance(
    onBackClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current
    var apptheme by rememberPreference(appTheme, AppThemeColor.System)
    var accentColorSource by rememberPreference(accentColorSource, AccentColorSource.Default )
    var progressBarStyle by rememberPreference(progressBarStyle, ProgressBar.Default )

    SettingsScreenLayout(
        title = stringResource(id = R.string.appearance),
        onBackClick = onBackClick,
        content = {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.theme),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard{

                EnumValueSelectorSettingsEntry(
                    title = stringResource(id = R.string.app_theme),
                    selectedValue = apptheme,
                    onValueSelected = { apptheme = it },
                    icon = IconSource.Icon( painterResource(id = R.drawable.dark_mode)),
                    valueText = { context.getString(it.resourceId) }
                )

                EnumValueSelectorSettingsEntry(
                    title = stringResource(id = R.string.accent_color),
                    selectedValue = accentColorSource,
                    onValueSelected = { accentColorSource = it },
                    icon = IconSource.Icon( painterResource(id = R.drawable.color_mode)),
                    valueText = { context.getString(it.resourceId) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Animations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorPalette.text.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard{

                EnumValueSelectorSettingsEntry(
                    title = stringResource(id = R.string.progress_bar_style),
                    selectedValue = progressBarStyle,
                    onValueSelected = { progressBarStyle = it },
                    icon = IconSource.Icon( painterResource(id = R.drawable.wave)),
                    valueText = { context.getString(it.resourceId) }
                )

                SettingColum(
                    icon = IconSource.Vector(Icons.Default.BlurOn),
                    title = "Background Style",
                    description = "Choose your preferred background style",
                    onClick = {},
                )
            }
        }
    )
}