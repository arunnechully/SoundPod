package com.github.soundpod.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    icon: ImageVector? = null,
    painterRes: Int? = null,
    switchState: Boolean,
    onSwitchChange: (Boolean) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colorPalette.text,
                modifier = Modifier.size(28.dp)
            )
        } else if (painterRes != null) {
            Icon(
                painter = painterResource(id = painterRes),
                contentDescription = title,
                tint = colorPalette.text,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colorPalette.text
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colorPalette.text.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = switchState,
            onCheckedChange = onSwitchChange,
            colors = SwitchDefaults.colors(checkedTrackColor = colorPalette.accent)
        )
    }
}