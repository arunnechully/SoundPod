package com.github.soundpod.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenLayout(
    title: String,
    description: String? = null,
    onBackClick: () -> Unit,
    scrollable: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    backgroundColor: Color = LocalAppearance.current.colorPalette.boxColor,
    backIcon: Int = R.drawable.arrow_back,
    horizontalPadding: Dp = 14.dp,
    actions: @Composable RowScope.() -> Unit = {},
    dropDownMenuContent: @Composable (ColumnScope.(dismissMenu: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    SettingsScreenLayout(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorPalette.text.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        onBackClick = onBackClick,
        scrollable = scrollable,
        shape = shape,
        backgroundColor = backgroundColor,
        backIcon = backIcon,
        horizontalPadding = horizontalPadding,
        actions = actions,
        dropDownMenuContent = dropDownMenuContent,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenLayout(
    title: @Composable (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    scrollable: Boolean = true,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    backgroundColor : Color = LocalAppearance.current.colorPalette.boxColor,
    backIcon: Int = R.drawable.arrow_back,
    horizontalPadding: Dp = 14.dp,
    actionsHorizontalPadding: Dp = 0.dp,
    actions: @Composable RowScope.() -> Unit = {},
    dropDownMenuContent: @Composable (ColumnScope.(dismissMenu: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val scrollState = rememberScrollState()
    var showDropDown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { title?.invoke() },
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
                actions = {
                    Row(
                        modifier = Modifier.padding(horizontal = actionsHorizontalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = colorPalette.text
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )

                .padding(horizontal = horizontalPadding)
                .clip(shape)
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreenLayout(
    title: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    dropDownMenuContent: @Composable (ColumnScope.(dismissMenu: () -> Unit) -> Unit)? = null,
    headerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
    onBackClick: (() -> Unit)? = null,
    backIcon: Int = R.drawable.arrow_back,
) {
    val (colorPalette) = LocalAppearance.current
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues(density).calculateTopPadding()
    val topBarHeight = 64.dp + statusBarHeight
    val topBarHeightPx = with(density) { topBarHeight.toPx() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fullHeightPx = constraints.maxHeight.toFloat()
        val peekHeightPx = fullHeightPx * 0.33f

        val sheetOffset = remember { Animatable(peekHeightPx) }
        val scope = rememberCoroutineScope()

        val isAtTop by remember {
            derivedStateOf { sheetOffset.value <= topBarHeightPx + 1f }
        }

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    return if (delta < 0 && sheetOffset.value > topBarHeightPx) {
                        val newOffset = (sheetOffset.value + delta).coerceAtLeast(topBarHeightPx)
                        val consumed = newOffset - sheetOffset.value
                        scope.launch {
                            sheetOffset.snapTo(newOffset)
                        }
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
                    return if (delta > 0 && sheetOffset.value < peekHeightPx) {
                        val newOffset = (sheetOffset.value + delta).coerceAtMost(peekHeightPx)
                        val consumedBySheet = newOffset - sheetOffset.value
                        scope.launch {
                            sheetOffset.snapTo(newOffset)
                        }
                        Offset(0f, consumedBySheet)
                    } else {
                        Offset.Zero
                    }
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    val velocityY = available.y
                    val target = if (kotlin.math.abs(velocityY) > 1000f) {
                        if (velocityY < 0) topBarHeightPx else peekHeightPx
                    } else {
                        if (sheetOffset.value < (peekHeightPx + topBarHeightPx) / 2) {
                            topBarHeightPx
                        } else {
                            peekHeightPx
                        }
                    }
                    scope.launch {
                        sheetOffset.animateTo(
                            targetValue = target,
                            initialVelocity = velocityY,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    return super.onPostFling(consumed, available)
                }
            }
        }

        var showDropDown by remember { mutableStateOf(false) }

        val progress by remember {
            derivedStateOf {
                ((sheetOffset.value - topBarHeightPx) / (peekHeightPx - topBarHeightPx)).coerceIn(0f, 1f)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { sheetOffset.value.toDp() })
                .graphicsLayer {
                    alpha = progress
                    val scale = 0.85f + (progress * 0.15f)
                    scaleX = scale
                    scaleY = scale
                    translationY = (sheetOffset.value - peekHeightPx) * 0.15f
                }
                .background(Color.Black.copy(alpha = (1f - progress) * 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            headerContent()
        }

        Surface(
            modifier = Modifier
                .offset { IntOffset(0, sheetOffset.value.roundToInt()) }
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = colorPalette.boxColor,
            shadowElevation = ((1f - progress) * 8).dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()

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
