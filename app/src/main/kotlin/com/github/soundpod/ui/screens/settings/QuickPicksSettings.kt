package com.github.soundpod.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.enums.QuickPicksSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.utils.quickPicksCustomGenreKey
import com.github.soundpod.utils.quickPicksSourceKey
import com.github.soundpod.utils.rememberPreference

@Composable
fun QuickPicksSettingsContent() {
    var quickPicksSource by rememberPreference(quickPicksSourceKey, QuickPicksSource.Default)
    var customGenre by rememberPreference(quickPicksCustomGenreKey, "Psaltic music")
    val focusManager = LocalFocusManager.current
    val (colorPalette) = LocalAppearance.current

    Column {
        SettingsCard {
            QuickPicksSource.entries.forEach { source ->
                QuickPicksSourceOption(
                    title = stringResource(id = source.resourceId),
                    selected = quickPicksSource == source,
                    onClick = { quickPicksSource = source },
                    showDivider = source != QuickPicksSource.entries.last()
                )
            }
        }

        AnimatedVisibility(
            visible = quickPicksSource == QuickPicksSource.Custom,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                SettingsGroup(title = stringResource(R.string.quick_picks_custom_genre)) {
                    SettingsCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (customGenre.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.quick_picks_custom_genre_description),
                                    style = typography.bodyMedium,
                                    color = colorPalette.text.copy(alpha = 0.5f)
                                )
                            }
                            
                            BasicTextField(
                                value = customGenre,
                                onValueChange = { customGenre = it },
                                singleLine = true,
                                textStyle = typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = colorPalette.text,
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(colorPalette.text),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPicksSourceOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    val colorPalette = LocalAppearance.current.colorPalette

    SettingsColumn(
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorPalette.accent,
                    unselectedColor = colorPalette.text.copy(alpha = 0.6f)
                )
            )
        },
        title = title,
        onClick = onClick,
        showDivider = showDivider,
    )
}
