package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.ThemeSelectorDialog

@Composable
inline fun <reified T : Enum<T>> EnumValueSelectorSettingsEntry(
    title: String,
    selectedValue: T,
    crossinline onValueSelected: (T) -> Unit,
    icon: IconSource? = null,
    isEnabled: Boolean = true,
    crossinline valueText: @Composable (T) -> String = { it.name },
    noinline trailingContent: @Composable (() -> Unit)? = null,
) {
    ValueSelectorSettingsEntry(
        title = title,
        selectedValue = selectedValue,
        values = enumValues<T>().toList(),
        onValueSelected = onValueSelected,
        icon = icon,
        isEnabled = isEnabled,
        valueText = valueText,
        descriptionColor = LocalAppearance.current.colorPalette.accent,
        trailingContent = trailingContent,
    )
}

@Suppress("AssignedValueIsNeverRead")
@Composable
inline fun <T> ValueSelectorSettingsEntry(
    title: String,
    selectedValue: T,
    values: List<T>,
    crossinline onValueSelected: (T) -> Unit,
    icon: IconSource? = null,
    isEnabled: Boolean = true,
    crossinline valueText: @Composable (T) -> String = { it.toString() },
    noinline trailingContent: @Composable (() -> Unit)? = null,
    descriptionColor: Color = LocalAppearance.current.colorPalette.text
) {
    var isShowingDialog by remember { mutableStateOf(false) }

    if (isShowingDialog) {
        ThemeSelectorDialog(
            title = title,
            selectedValue = selectedValue,
            values = values,
            onValueSelected = {
                onValueSelected(it)
            },
            onDismiss = { isShowingDialog = false },
            valueText = valueText
        )

    }

    SettingsColumn(
        icon = icon,
        title = title,
        description = valueText(selectedValue),
        descriptionColor = descriptionColor,
        onClick = { isShowingDialog = true },
        isEnabled = isEnabled,
        trailingContent = trailingContent,
    )
}

@Composable
fun SettingsInformation(
    text: String,
) {
    Column(
        modifier = Modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingsProgress(
    text: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colorPalette.text.copy(alpha = 0.8f)
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(CircleShape),
            trackColor = colorPalette.text.copy(alpha = 0.1f),
            color = colorPalette.accent
        )

        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = colorPalette.accent
        )
    }
}


@Composable
fun SettingRow(
    title: String,
    icon: IconSource? = null,
    onClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        when (icon) {
            is IconSource.Vector -> Icon(
                imageVector = icon.imageVector,
                contentDescription = title,
                tint = colorPalette.text,
                modifier = Modifier.size(28.dp)
            )

            is IconSource.Icon -> Icon(
                painter = icon.painter,
                contentDescription = title,
                tint = colorPalette.text,
                modifier = Modifier.size(28.dp),
            )

            null -> {}
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colorPalette.text
            )
        }
    }
}


@Composable
fun SettingsColumn(
    icon: IconSource? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    title: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
    showDivider: Boolean = false,
    descriptionColor: Color = LocalAppearance.current.colorPalette.text.copy(alpha = 0.7f)
) {
    val (colorPalette) = LocalAppearance.current

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            enabled = isEnabled,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(start = 12.dp, end = 12.dp)
            .graphicsLayer { alpha = if (isEnabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(12.dp))
        } else if (icon != null) {
            when (icon) {
                is IconSource.Vector -> Icon(
                    imageVector = icon.imageVector,
                    contentDescription = title,
                    tint = colorPalette.text,
                    modifier = Modifier.size(28.dp)
                )
                is IconSource.Icon -> Icon(
                    painter = icon.painter,
                    contentDescription = title,
                    tint = colorPalette.text,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorPalette.text
                    )
                    if (description != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = descriptionColor
                        )
                    }
                }
                trailingContent?.invoke()
            }
            if (showDivider) {
                HorizontalDivider(
                    color = colorPalette.text.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun SettingsAlertDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    alertMessage: String
) {
    val colorPalette = LocalAppearance.current.colorPalette

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = colorPalette.boxColor,
        title = {
            Text(
                text = title,
                color = colorPalette.text
            )
        },
        text = {
            Text(
                text = alertMessage,
                color = colorPalette.text.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmClick
            ) {
                Text(
                    text = stringResource(android.R.string.ok),
                    color = colorPalette.text
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(
                    text = stringResource(android.R.string.cancel),
                    color = colorPalette.text
                )
            }
        }
    )
}


@Composable
fun SettingsGroup(
    title: String? = null,
    content: @Composable () -> Unit
) {
    if (title != null) {

        val (colorPalette) = LocalAppearance.current

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = colorPalette.text.copy(alpha = 0.7f)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    SettingsCard {
        content()
    }
}