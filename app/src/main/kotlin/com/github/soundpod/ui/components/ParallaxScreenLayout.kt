package com.github.soundpod.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerPadding
import com.github.soundpod.R
import com.github.soundpod.utils.thumbnail
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParallaxScreenLayout(
    title: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    dropDownMenuContent: @Composable (ColumnScope.(dismissMenu: () -> Unit) -> Unit)? = null,
    isLoading: Boolean = false,
    thumbnailUrl: String? = null,
    showThumbnail: Boolean = true,
    headerCustomContent: @Composable (ColumnScope.() -> Unit)? = null,
    footerHeaderContent: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
    onBackClick: (() -> Unit)? = null,
    backIcon: Int = R.drawable.arrow_back,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    headerTitle: String? = null,

    ) {
    val (colorPalette) = LocalAppearance.current
    val density = LocalDensity.current

    Scaffold(
        containerColor = colorPalette.background4,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        val playerPadding = LocalPlayerPadding.current
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues(density).calculateTopPadding()
        val topBarHeight = 64.dp + statusBarHeight
        val topBarHeightPx = with(density) { topBarHeight.toPx() }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding() + playerPadding)
        ) {
            val fullHeightPx = constraints.maxHeight.toFloat()
            // Dynamic peek height: approximately 45% of screen height, but with sensible bounds
            val peekHeightPx = remember(fullHeightPx) {
                (fullHeightPx * 0.42f).coerceIn(
                    with(density) { 320.dp.toPx() },
                    with(density) { 480.dp.toPx() }
                )
            }

            var sheetOffset by remember(peekHeightPx) { mutableFloatStateOf(peekHeightPx) }
            val offsetAnimatable = remember { Animatable(sheetOffset) }
            val scope = rememberCoroutineScope()

            val isAtTop by remember {
                derivedStateOf { sheetOffset <= (topBarHeightPx + 2f) }
            }

            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        val delta = available.y

                        if (source == NestedScrollSource.UserInput) {
                            scope.launch { offsetAnimatable.stop() }
                        }

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

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity
                    ): Velocity {
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
                            offsetAnimatable.snapTo(sheetOffset)
                            offsetAnimatable.animateTo(
                                targetValue = target,
                                initialVelocity = velocityY,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) {
                                sheetOffset = this.value
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
                    ((sheetOffset - topBarHeightPx) / (peekHeightPx - topBarHeightPx)).coerceIn(
                        0f,
                        1f
                    )
                }
            }



            // Header Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { peekHeightPx.toDp() })
                    .graphicsLayer {
                        alpha = progress
                        translationY = (sheetOffset - peekHeightPx) * 0.4f
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Background Thumbnail (Full Bleed)
                AsyncImage(
                    model = thumbnailUrl?.thumbnail(1024),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Scrim for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.7f),
                                0.2f to Color.Black.copy(alpha = 0.3f),
                                0.5f to Color.Transparent,
                                0.75f to colorPalette.background4.copy(alpha = 0.8f),
                                1f to colorPalette.background4
                            )
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                        .padding(horizontal = 24.dp)
                ) {
                    if (showThumbnail) {
                        AdaptiveThumbnail(
                            isLoading = isLoading,
                            url = thumbnailUrl,
                            modifier = Modifier.fillMaxWidth(0.55f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Text(
                        text = headerTitle ?: "",
                        style = typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                blurRadius = 8f,
                                offset = Offset(2f, 2f)
                            )
                        ),
                        color = colorPalette.text,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    headerCustomContent?.invoke(this)
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, sheetOffset.roundToInt()) }
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    footerHeaderContent?.invoke()
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = shape,
                        color = colorPalette.mainBackground,
                        shadowElevation = ((1f - progress) * 12).dp
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            content()
                        }
                    }
                }
            }

            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isAtTop) colorPalette.background4 else Color.Transparent,
                    scrolledContainerColor = colorPalette.background4,
                    navigationIconContentColor = colorPalette.text,
                    titleContentColor = colorPalette.text,
                    actionIconContentColor = colorPalette.text
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
                                onDismissRequest = { showDropDown = false },
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