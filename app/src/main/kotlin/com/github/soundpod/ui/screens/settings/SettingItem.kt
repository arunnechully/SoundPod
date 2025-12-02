package com.github.soundpod.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.ThemeSelectorDialog

@Composable
inline fun <reified T : Enum<T>> EnumValueSelectorSettingsEntry(
    title: String,
    selectedValue: T,
    crossinline onValueSelected: (T) -> Unit,
    icon: IconSource? = null,
    isEnabled: Boolean = true,
    crossinline valueText: (T) -> String = Enum<T>::name,
    noinline trailingContent: @Composable (() -> Unit)? = null
) {
    ValueSelectorSettingsEntry(
        title = title,
        selectedValue = selectedValue,
        values = enumValues<T>().toList(),
        onValueSelected = onValueSelected,
        icon = icon,
        isEnabled = isEnabled,
        valueText = valueText,
        trailingContent = trailingContent,
    )
}

@Composable
inline fun <T> ValueSelectorSettingsEntry(
    title: String,
    selectedValue: T,
    values: List<T>,
    crossinline onValueSelected: (T) -> Unit,
    icon: IconSource? = null,
    isEnabled: Boolean = true,
    crossinline valueText: (T) -> String = { it.toString() },
    noinline trailingContent: @Composable (() -> Unit)? = null
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

    SettingColum(
        icon = icon,
        title = title,
        description = valueText(selectedValue),
        onClick = { isShowingDialog = true },
        isEnabled = isEnabled,
        trailingContent = trailingContent
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
fun SettingsProgress(text: String, progress: Float) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.width(240.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
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
fun SettingColum(
    icon: IconSource? = null,
    title: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
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

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colorPalette.text
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorPalette.text.copy(alpha = 0.7f)
                )
            }
        }

        trailingContent?.invoke()
    }
}
