package com.github.musicyou.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.musicyou.models.ActionInfo
import com.github.musicyou.utils.listGesturesEnabledKey
import com.github.musicyou.utils.rememberPreference
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox

@Composable
fun SwipeToActionBox(
    modifier: Modifier = Modifier,
    primaryAction: ActionInfo? = null,
    destructiveAction: ActionInfo? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var listGesturesEnabled by rememberPreference(listGesturesEnabledKey, true)

    val startAction = primaryAction?.let { action ->
        SwipeAction(
            onSwipe = action.onClick,
            icon = {
                Icon(
                    imageVector = action.icon,
                    contentDescription = stringResource(id = action.description),
                    modifier = Modifier.padding(end = 32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            },
            background = MaterialTheme.colorScheme.primaryContainer
        )
    }

    val endAction = destructiveAction?.let { action ->
        SwipeAction(
            onSwipe = action.onClick,
            icon = {
                Icon(
                    imageVector = action.icon,
                    contentDescription = stringResource(id = action.description),
                    modifier = Modifier.padding(start = 32.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            },
            background = MaterialTheme.colorScheme.errorContainer
        )
    }

    if (listGesturesEnabled) {
        SwipeableActionsBox(
            modifier = modifier,
            startActions = startAction?.let { action -> listOf(action) } ?: emptyList(),
            endActions = endAction?.let { action -> listOf(action) } ?: emptyList(),
            content = content
        )
    } else {
        Box(
            modifier = modifier,
            content = content
        )
    }
}