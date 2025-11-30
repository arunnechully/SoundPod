package com.github.soundpod.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.github.soundpod.ui.screens.player.PlayerScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWrapper(
    onNavigateToSettings: () -> Unit
) {
    val musicNavController = rememberNavController()
    val scope = rememberCoroutineScope()

    val playerState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    BackHandler(enabled = playerState.currentValue == SheetValue.Expanded) {
        scope.launch { playerState.partialExpand() }
    }

    PlayerScaffold(
        navController = musicNavController,
        sheetState = playerState,
        scaffoldPadding = PaddingValues(0.dp),
        showPlayer = true
    ) {
        MusicNavigation(
            navController = musicNavController,
            sheetState = playerState,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}