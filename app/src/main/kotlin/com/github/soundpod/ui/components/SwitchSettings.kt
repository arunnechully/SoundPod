package com.github.soundpod.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.github.core.ui.LocalAppearance
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.screens.settings.SettingsColumn

@Composable
fun SwitchSetting(
    title: String,
    description: String? = null,
    icon: IconSource? = null,
    switchState: Boolean,
    isEnabled: Boolean = true,
    onSwitchChange: (Boolean) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val haptic = LocalHapticFeedback.current

    SettingsColumn(
        title = title,
        description = description,
        icon = icon,
        isEnabled = isEnabled,
        trailingContent = {
            Switch(
                checked = switchState,
                onCheckedChange = { checked ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSwitchChange(checked)
                },
                enabled = isEnabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colorPalette.accent,
                    checkedThumbColor = colorPalette.onAccent,
                    uncheckedThumbColor = colorPalette.onAccent
                )
            )
        },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSwitchChange(!switchState)
        }
    )
}