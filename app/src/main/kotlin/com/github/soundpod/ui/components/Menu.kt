package com.github.soundpod.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.soundpod.ui.styling.Dimensions

@Composable
inline fun Menu(
    modifier: Modifier = Modifier,
    crossinline content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuEntry(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    secondaryText: String? = null,
    enabled: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(text = text)
        },
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1F else Dimensions.lowOpacity),
        supportingContent = {
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    modifier = Modifier.alpha(Dimensions.mediumOpacity)
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = text
            )
        },
        trailingContent = {
            if (trailingContent != null) {
                trailingContent()
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = BottomSheetDefaults.ContainerColor
        )
    )
}