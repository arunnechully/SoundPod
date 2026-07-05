package com.github.soundpod.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.models.SortBy
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.screens.settings.SettingRow

@Composable
fun <T : SortBy> SortingHeader(
    sortBy: T,
    changeSortBy: (T) -> Unit,
    sortByEntries: List<T>,
    onPlayClick: (() -> Unit)? = null,
    onShuffleClick: (() -> Unit)? = null
) {
    var isSorting by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingRow(
            icon = IconSource.Icon(painterResource(id = R.drawable.sorting)),
            title = stringResource(id = sortBy.text),
            onClick = { isSorting = true },
            modifier = Modifier.clip(shape = RoundedCornerShape(18.dp)),
            fillMaxWidth = false,
            padding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
        if (onPlayClick != null) {
            ActionButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = "Play All",
                onClick = onPlayClick
            )
        }
        if (onPlayClick != null && onShuffleClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Optional Shuffle Button
        if (onShuffleClick != null) {
            ActionButton(
                icon = Icons.Default.Shuffle,
                contentDescription = "Shuffle Play",
                onClick = onShuffleClick
            )
        }

        CustomDropdownMenu(
            expanded = isSorting,
            onDismissRequest = { isSorting = false },
            topPadding = 0.dp
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

                        if (sortBy == entry) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected"
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorPalette = LocalAppearance.current.colorPalette
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = colorPalette.glass,
        ),
        modifier = modifier
            .size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .size(22.dp)
        )
    }
}