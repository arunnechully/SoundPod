//package com.github.soundpod.ui.components
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.animateColorAsState
//import androidx.compose.animation.core.animateDpAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.ColumnScope
//import androidx.compose.foundation.layout.RowScope
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.offset
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.rememberLazyListState
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.MoreVert
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.derivedStateOf
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableFloatStateOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
//import androidx.compose.ui.input.nestedscroll.NestedScrollSource
//import androidx.compose.ui.input.nestedscroll.nestedScroll
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.IntOffset
//import androidx.compose.ui.unit.dp
//import com.github.core.ui.LocalAppearance
//import com.github.soundpod.R
//import kotlin.math.roundToInt
//
//// Ensure your LocalAppearance and CustomDropdownMenu imports are here
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PlaylistScreenLayout(
//    title: @Composable (() -> Unit)? = null,
//    actions: @Composable RowScope.() -> Unit = {},
//    dropDownMenuContent: @Composable (ColumnScope.(dismissMenu: () -> Unit) -> Unit)? = null,
//    headerContent: @Composable () -> Unit,
//    content: @Composable () -> Unit,
//    onBackClick: (() -> Unit)? = null,
//) {
//    val (colorPalette) = LocalAppearance.current
//    var showDropDown by remember { mutableStateOf(false) }
//
//    // 1. Setup physics boundaries
//    val density = LocalDensity.current
//    val headerHeight = 360.dp
//    val maxScrollPx = with(density) { headerHeight.toPx() }
//    var scrollOffsetPx by remember { mutableFloatStateOf(0f) }
//
//    // 2. The magic connection that steals the scroll from the child LazyColumn
//    val nestedScrollConnection = remember {
//        object : NestedScrollConnection {
//            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
//                val delta = available.y
//                if (delta < 0) { // Swiping UP
//                    val newOffset = scrollOffsetPx - delta
//                    val previousOffset = scrollOffsetPx
//                    scrollOffsetPx = newOffset.coerceIn(0f, maxScrollPx)
//                    val consumed = scrollOffsetPx - previousOffset
//                    return Offset(0f, -consumed)
//                }
//                return Offset.Zero
//            }
//
//            override fun onPostScroll(
//                consumed: Offset,
//                available: Offset,
//                source: NestedScrollSource
//            ): Offset {
//                val delta = available.y
//                if (delta > 0) { // Swiping DOWN
//                    val newOffset = scrollOffsetPx - delta
//                    val previousOffset = scrollOffsetPx
//                    scrollOffsetPx = newOffset.coerceIn(0f, maxScrollPx)
//                    val consumedOffset = scrollOffsetPx - previousOffset
//                    return Offset(0f, -consumedOffset)
//                }
//                return Offset.Zero
//            }
//        }
//    }
//
//    // 3. UI States driven by the physics engine
//    val isBoxAtTop by remember {
//        derivedStateOf { scrollOffsetPx >= maxScrollPx }
//    }
//    val cornerRadius by animateDpAsState(
//        targetValue = if (isBoxAtTop) 0.dp else 24.dp,
//        animationSpec = tween(200),
//        label = "cornerRadius"
//    )
//    val topBarColor by animateColorAsState(
//        targetValue = if (isBoxAtTop) colorPalette.boxColor else Color.Transparent,
//        animationSpec = tween(200),
//        label = "topBarColor"
//    )
//
//    Scaffold(
//        containerColor = MaterialTheme.colorScheme.background,
//        topBar = {
//            TopAppBar(
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = topBarColor,
//                    scrolledContainerColor = topBarColor
//                ),
//                navigationIcon = {
//                    if (onBackClick != null) {
//                        IconButton(onClick = onBackClick) {
//                            Icon(
//                                painter = painterResource(R.drawable.arrow_back),
//                                contentDescription = "Back",
//                                modifier = Modifier.size(18.dp),
//                                tint = colorPalette.text
//                            )
//                        }
//                    }
//                },
//                title = {
//                    AnimatedVisibility(
//                        visible = isBoxAtTop,
//                        enter = fadeIn(animationSpec = tween(300)),
//                        exit = fadeOut(animationSpec = tween(200))
//                    ) {
//                        title?.invoke()
//                    }
//                },
//                actions = {
//                    actions()
//                    if (dropDownMenuContent != null) {
//                        Box(contentAlignment = Alignment.TopEnd) {
//                            IconButton(onClick = { showDropDown = true }) {
//                                Icon(
//                                    imageVector = Icons.Default.MoreVert,
//                                    contentDescription = "Options",
//                                    tint = colorPalette.text,
//                                    modifier = Modifier.size(24.dp)
//                                )
//                            }
//                            CustomDropdownMenu(
//                                expanded = showDropDown,
//                                onDismissRequest = { showDropDown = false }
//                            ) {
//                                dropDownMenuContent { showDropDown = false }
//                            }
//                        }
//                    }
//                },
//            )
//        }
//    ) { innerPadding ->
//        // This outer box acts as the physics boundary
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .nestedScroll(nestedScrollConnection)
//        ) {
//
//            // LAYER 1: Background Header
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(headerHeight)
//                    .graphicsLayer {
//                        translationY = -scrollOffsetPx * 0.5f // Smooth Parallax
//                        alpha = 1f - (scrollOffsetPx / maxScrollPx) // Smooth Fade
//                    }
//            ) {
//                headerContent()
//            }
//
//            // LAYER 2: Foreground Rounded Box
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    // We use offset block for buttery 60fps performance (avoids recomposition)
//                    .offset { IntOffset(0, (maxScrollPx - scrollOffsetPx).roundToInt()) }
//                    .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
//                    .background(colorPalette.boxColor)
//                    .padding(top = 16.dp)
//            ) {
//                // Your LazyColumn from NewLocalPlaylistSongs goes right here!
//                content()
//            }
//        }
//    }
//}