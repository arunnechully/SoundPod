package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.soundpod.ui.styling.Dimensions

/**
 * Shared metrics for the expanded player.
 *
 * [SharedThumbnail] is drawn as an overlay on top of [MainPlayerContent] and is positioned with an
 * absolute offset, so it cannot rely on the layout pass to stay aligned with the placeholder that
 * [MainPlayerContent] reserves for it. These values keep the two in sync.
 */
internal object PlayerMetrics {

    /** Height of the row rendered by `PlayerTopControl`, excluding the status bar inset. */
    val topControlHeight: Dp = 48.dp

    /**
     * Distance between the top of the expanded player and the top of the artwork, matching the
     * placeholder in [MainPlayerContent]: a spacer, the top control row (which is itself pushed
     * down by the status bar inset), and another spacer.
     */
    val expandedThumbnailTop: Dp
        @Composable
        get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                Dimensions.spacer +
                topControlHeight +
                Dimensions.spacer
}
