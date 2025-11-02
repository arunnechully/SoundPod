package com.github.soundpod.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.soundpod.models.ActionInfo

@Composable
fun CoverScaffold(
    primaryButton: ActionInfo,
    secondaryButton: ActionInfo,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.widthIn(max = 400.dp)
    ) {
        content()

        if (primaryButton.enabled) {
            FloatingActionButton(
                onClick = primaryButton.onClick,
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Icon(
                    imageVector = primaryButton.icon,
                    contentDescription = stringResource(id = primaryButton.description)
                )
            }
        }

        if (secondaryButton.enabled) {
            SmallFloatingActionButton(
                onClick = secondaryButton.onClick,
                modifier = Modifier.align(Alignment.TopEnd),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(
                    imageVector = secondaryButton.icon,
                    contentDescription = stringResource(id = secondaryButton.description)
                )
            }
        }
    }
}