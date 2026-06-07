package com.github.soundpod.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreenLayout(
    title: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    dropDownMenuContent: @Composable (ColumnScope.(dismissMenu: () -> Unit) -> Unit)? = null,
    headerContent: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    onBackClick: (() -> Unit)? = null,
    backIcon: Int = R.drawable.arrow_back,
    shape: Shape = MaterialTheme.shapes.extraLarge,
) {
    val (colorPalette) = LocalAppearance.current
    val density = LocalDensity.current

    Scaffold(
        containerColor = colorPalette.background4,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues(density).calculateTopPadding()
        val topBarHeight = 64.dp + statusBarHeight
        val topBarHeightPx = with(density) { topBarHeight.toPx() }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            val fullHeightPx = constraints.maxHeight.toFloat()
            val peekHeightPx = fullHeightPx * 0.45f

            var sheetOffset by remember(peekHeightPx) { mutableFloatStateOf(peekHeightPx) }
            val scope = rememberCoroutineScope()

            val isAtTop by remember {
                derivedStateOf { sheetOffset <= topBarHeightPx + 1f }
            }

            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val delta = available.y
                        return if (delta < 0 && sheetOffset > topBarHeightPx) {
                            val newOffset = (sheetOffset + delta).coerceAtLeast(topBarHeightPx)
                            val consumed = newOffset - sheetOffset
                            sheetOffset = newOffset
                            Offset(0f, consumed)
                        } else {
                            Offset.Zero
                        }
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val delta = available.y
                        return if (delta > 0 && sheetOffset < peekHeightPx) {
                            val newOffset = (sheetOffset + delta).coerceAtMost(peekHeightPx)
                            val consumedBySheet = newOffset - sheetOffset
                            sheetOffset = newOffset
                            Offset(0f, consumedBySheet)
                        } else {
                            Offset.Zero
                        }
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        val velocityY = available.y
                        val target = if (abs(velocityY) > 1000f) {
                            if (velocityY < 0) topBarHeightPx else peekHeightPx
                        } else {
                            if (sheetOffset < (peekHeightPx + topBarHeightPx) / 2) {
                                topBarHeightPx
                            } else {
                                peekHeightPx
                            }
                        }
                        if (sheetOffset != target) {
                            scope.launch {
                                Animatable(sheetOffset).animateTo(
                                    targetValue = target,
                                    initialVelocity = velocityY,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) {
                                    sheetOffset = this.value
                                }
                            }
                            return Velocity(0f, velocityY)
                        }
                        return super.onPostFling(consumed, available)
                    }
                }
            }

            var showDropDown by remember { mutableStateOf(false) }

            val progress by remember {
                derivedStateOf {
                    ((sheetOffset - topBarHeightPx) / (peekHeightPx - topBarHeightPx)).coerceIn(0f, 1f)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { sheetOffset.toDp() })
                    .graphicsLayer {
                        alpha = progress
                        val scale = 0.85f + (progress * 0.15f)
                        scaleX = scale
                        scaleY = scale
                        translationY = (sheetOffset - peekHeightPx) * 0.15f + (topBarHeightPx / 2f) * progress
                    },
                contentAlignment = Alignment.Center
            ) {
                headerContent()
            }

            Surface(
                modifier = Modifier
                    .offset { IntOffset(0, sheetOffset.roundToInt()) }
                    .fillMaxSize()
                    .padding(
                        bottom = innerPadding.calculateBottomPadding()
                    )
                    .nestedScroll(nestedScrollConnection),
//            shape = shape,
                color = colorPalette.boxColor,
                shadowElevation = ((1f - progress) * 8).dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
//                    .background(color = colorPalette.red)

//                    .clip(shape)
                ) {
                    content()
                }
            }

            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(backIcon),
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp),
                                tint = colorPalette.text
                            )
                        }
                    }
                },
                title = {
                    AnimatedVisibility(
                        visible = isAtTop,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        title?.invoke()
                    }
                },
                actions = {
                    actions()
                    if (dropDownMenuContent != null) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { showDropDown = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = colorPalette.text,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            CustomDropdownMenu(
                                expanded = showDropDown,
                                onDismissRequest = { showDropDown = false }
                            ) {
                                dropDownMenuContent { showDropDown = false }
                            }
                        }
                    }
                }
            )
        }
    }
}