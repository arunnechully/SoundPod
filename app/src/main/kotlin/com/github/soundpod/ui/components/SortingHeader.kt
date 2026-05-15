package com.github.soundpod.ui.components

import androidx.annotation.PluralsRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.soundpod.enums.SortOrder
import com.github.soundpod.models.SortBy

@Composable
fun <T : SortBy> SortingHeader(
    sortBy: T,
    changeSortBy: (T) -> Unit,
    sortByEntries: List<T>,
    sortOrder: SortOrder,
    toggleSortOrder: (SortOrder) -> Unit,
    size: Int = 0,
    @PluralsRes itemCountText: Int? = null,
    onPlayClick: (() -> Unit)? = null,
    onShuffleClick: (() -> Unit)? = null
) {
    var isSorting by rememberSaveable { mutableStateOf(false) }
    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        label = "rotation"
    )

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { isSorting = true }
        ) {
            Text(text = stringResource(id = sortBy.text))
        }

        IconButton(
            onClick = { toggleSortOrder(!sortOrder) },
            modifier = Modifier.graphicsLayer { rotationZ = sortOrderIconRotation }
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.weight(1F))
        if (itemCountText != null) {
            Text(
                text = pluralStringResource(id = itemCountText, count = size, size),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        // Optional Play Button
        if (onPlayClick != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play All",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (onPlayClick != null && onShuffleClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Optional Shuffle Button
        if (onShuffleClick != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { onShuffleClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle Play",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = isSorting,
            onDismissRequest = { isSorting = false }
        ) {
            sortByEntries.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(id = entry.text))
                    },
                    onClick = {
                        isSorting = false
                        changeSortBy(entry)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = stringResource(id = entry.text)
                        )
                    },
                    trailingIcon = {
                        RadioButton(
                            selected = sortBy == entry,
                            onClick = { changeSortBy(entry) }
                        )
                    }
                )
            }
        }
    }
}