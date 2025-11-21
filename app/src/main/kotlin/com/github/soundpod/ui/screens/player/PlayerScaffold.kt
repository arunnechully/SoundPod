package com.github.soundpod.ui.screens.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScaffold(
    navController: NavController,
    sheetState: SheetState,
    scaffoldPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val (colorPalette) = LocalAppearance.current
    Box(
        modifier = Modifier.windowInsetsPadding(
            WindowInsets(
                left = scaffoldPadding.calculateLeftPadding(layoutDirection),
                right = scaffoldPadding.calculateRightPadding(layoutDirection)
            )
        )
    ) {
        BottomSheetScaffold(
            sheetContent = {
                AnimatedContent(
                    targetState = sheetState.targetValue,
                    label = "player",
                    contentKey = { value ->
                        if (value == SheetValue.Expanded) 0 else 1
                    }
                ) { value ->
                    if (value == SheetValue.Expanded) {
                        NewPlayer(
                            onGoToAlbum = { browseId ->
                                scope.launch { sheetState.partialExpand() }
                                navController.navigate(
                                    route = Routes.Album(id = browseId)
                                )
                            },
                            onGoToArtist = { browseId ->
                                scope.launch { sheetState.partialExpand() }
                                navController.navigate(
                                    route = Routes.Artist(id = browseId)
                                )
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NewMiniPlayer(
                                openPlayer = {
                                    scope.launch { sheetState.expand() }
                                }
                            )
                        }

                    }
                }
            },
            scaffoldState = scaffoldState,
            sheetPeekHeight = 40.dp + 20.dp + scaffoldPadding.calculateBottomPadding(),
            sheetMaxWidth = Int.MAX_VALUE.dp,
            sheetDragHandle = null,

            //change player background color
            sheetContainerColor = colorPalette.background1

        ) {
            val bottomPadding = animateDpAsState(
                targetValue = if (sheetState.currentValue == SheetValue.Hidden) scaffoldPadding.calculateBottomPadding() else scaffoldPadding.calculateBottomPadding() + 76.dp + 16.dp,
                label = "padding"
            )

            CompositionLocalProvider(value = LocalPlayerPadding provides bottomPadding.value) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    content = content
                )
            }
        }
    }
}