package com.github.musicyou.ui.screens.builtinplaylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.musicyou.enums.BuiltInPlaylist

@Composable
fun BuiltInPlaylistThumbnail(builtInPlaylist: BuiltInPlaylist) {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        //val thumbnailSizeDp = if (isLandscape) (maxHeight - 128.dp) else (maxWidth - 64.dp)
        val thumbnailSizeDp = maxWidth - 64.dp

        Box(
            modifier = Modifier
                .padding(all = 16.dp)
                .clip(MaterialTheme.shapes.large)
                .size(thumbnailSizeDp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (builtInPlaylist) {
                    BuiltInPlaylist.Favorites -> Icons.Default.Favorite
                    BuiltInPlaylist.Offline -> Icons.Default.DownloadForOffline
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(thumbnailSizeDp / 2)
            )
        }
    }
}