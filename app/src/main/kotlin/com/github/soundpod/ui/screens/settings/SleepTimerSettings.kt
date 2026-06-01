package com.github.soundpod.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.ui.common.IconSource
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SwitchSetting
import com.github.soundpod.utils.rememberPreference
import com.github.soundpod.utils.selectedSleepTimerPresetKey
import com.github.soundpod.utils.stopAfterCurrentKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@Suppress("AssignedValueIsNeverRead")
@Composable
fun SleepTimerSettingsContent() {
    val binder = LocalPlayerServiceBinder.current
    val sleepTimerMillisLeft by (binder?.sleepTimerMillisLeft
        ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    var stopAfterCurrent by rememberPreference(stopAfterCurrentKey, false)

    var isCustomDialogShowing by remember { mutableStateOf(false) }

    var selectedPreset by rememberPreference(selectedSleepTimerPresetKey, -1)

    LaunchedEffect(sleepTimerMillisLeft) {
        if (sleepTimerMillisLeft == null) {
            selectedPreset = -1
        }
    }

    if (isCustomDialogShowing) {
        CustomSleepTimerDialog(
            onDismiss = { isCustomDialogShowing = false },
            onDone = { hours, minutes ->
                val millis = (hours * 3600 + minutes * 60) * 1000L
                if (millis > 0) {
                    binder?.startSleepTimer(millis)
                    selectedPreset = 4
                } else {
                    binder?.cancelSleepTimer()
                    selectedPreset = -1
                }
                isCustomDialogShowing = false
            }
        )
    }

    Column {
        SettingsCard {
            SleepTimerOption(
                title = stringResource(id = R.string.off),
                selected = sleepTimerMillisLeft == null,
                isEnabled = !stopAfterCurrent,
                onClick = {
                    binder?.cancelSleepTimer()
                    selectedPreset = -1
                }
            )

            SleepTimerOption(
                title = stringResource(id = R.string.minutes_30),
                selected = selectedPreset == 0 || (sleepTimerMillisLeft != null && sleepTimerMillisLeft!! > 29 * 60 * 1000L && sleepTimerMillisLeft!! <= 30 * 60 * 1000L),
                isEnabled = !stopAfterCurrent,
                onClick = {
                    binder?.startSleepTimer(30 * 60 * 1000L)
                    selectedPreset = 0
                }
            )

            SleepTimerOption(
                title = stringResource(id = R.string.hour_1),
                selected = selectedPreset == 1 || (sleepTimerMillisLeft != null && sleepTimerMillisLeft!! > 59 * 60 * 1000L && sleepTimerMillisLeft!! <= 60 * 60 * 1000L),
                isEnabled = !stopAfterCurrent,
                onClick = {
                    binder?.startSleepTimer(60 * 60 * 1000L)
                    selectedPreset = 1
                }
            )

            SleepTimerOption(
                title = stringResource(id = R.string.hour_1_30),
                selected = selectedPreset == 2 || (sleepTimerMillisLeft != null && sleepTimerMillisLeft!! > 89 * 60 * 1000L && sleepTimerMillisLeft!! <= 90 * 60 * 1000L),
                isEnabled = !stopAfterCurrent,
                onClick = {
                    binder?.startSleepTimer(90 * 60 * 1000L)
                    selectedPreset = 2
                }
            )

            SleepTimerOption(
                title = stringResource(id = R.string.hours_2),
                selected = selectedPreset == 3 || (sleepTimerMillisLeft != null && sleepTimerMillisLeft!! > 119 * 60 * 1000L && sleepTimerMillisLeft!! <= 120 * 60 * 1000L),
                isEnabled = !stopAfterCurrent,
                onClick = {
                    binder?.startSleepTimer(120 * 60 * 1000L)
                    selectedPreset = 3
                }
            )

            SleepTimerOption(
                title = stringResource(id = R.string.custom),
                selected = selectedPreset == 4,
                isEnabled = !stopAfterCurrent,
                onClick = { isCustomDialogShowing = true },
                showDivider = false
            )
        }

        SettingsGroup {
            SwitchSetting(
                icon = IconSource.Vector(Icons.Outlined.Timer),
                title = stringResource(R.string.stop_after_current),
                description = stringResource(R.string.stop_after_current_description),
                switchState = stopAfterCurrent,
                onSwitchChange = {
                    stopAfterCurrent = it
                    if (it) {
                        binder?.cancelSleepTimer()
                        selectedPreset = -1
                    }
                }
            )
        }
    }
}

@Composable
fun SleepTimerOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
    showDivider: Boolean = true
) {
    val colorPalette = LocalAppearance.current.colorPalette

    SettingsColumn(
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = isEnabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colorPalette.accent, // Applies your custom color when checked
                    unselectedColor = colorPalette.text.copy(alpha = 0.6f) // Optional: custom unselected color
                )

            )
        },
        title = title,
        onClick = onClick,
        isEnabled = isEnabled,
        showDivider = showDivider,
    )
}

@Composable
fun CustomSleepTimerDialog(
    onDismiss: () -> Unit,
    onDone: (Int, Int) -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }

    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    fun animateAndClose(onFinished: () -> Unit) {
        isVisible = false
        scope.launch {
            delay(200)
            onFinished()
        }
    }

    Dialog(
        onDismissRequest = { animateAndClose(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            Surface(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = colorPalette.boxColor
            ) {
                Column(
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.custom),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = colorPalette.text
                        ),
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        NumberPicker(
                            range = 0..23,
                            initialValue = hours,
                            onValueChange = { hours = it },
                            modifier = Modifier.width(70.dp)
                        )

                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NumberPicker(
                            range = 0..59,
                            initialValue = minutes,
                            onValueChange = { minutes = it },
                            modifier = Modifier.width(70.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { animateAndClose(onDismiss) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.cancel),
                                color = colorPalette.text,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight(0.6f)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )

                        TextButton(
                            onClick = { animateAndClose { onDone(hours, minutes) } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(id = R.string.done),
                                color = colorPalette.text,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPicker(
    range: IntRange,
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = range.last - range.first + 1
    val targetIndex = (initialValue - range.first) + 500 * size
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = targetIndex - 1 - 12)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeight = 60.dp
    val visibleItems = 3
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    LaunchedEffect(Unit) {
        listState.animateScrollToItem(targetIndex - 1)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { onValueChange(range.first + ((it + 1) % size)) }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 4.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.medium
        ) {}

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(1000 * size) { index ->
                val value = range.first + (index % size)

                val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                val activeColor = MaterialTheme.colorScheme.primary

                val scaleAndColor by remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                        if (itemInfo != null) {
                            val viewportCenter =
                                (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2f
                            val itemCenter = itemInfo.offset + itemInfo.size / 2f
                            val distanceFromCenter = abs(viewportCenter - itemCenter)
                            val fraction = (1f - (distanceFromCenter / itemHeightPx)).coerceIn(0f, 1f)

                            val scale = 0.8f + (0.4f * fraction)
                            val color = lerp(
                                start = inactiveColor,
                                stop = activeColor,
                                fraction = fraction
                            )
                            scale to color
                        } else {
                            0.8f to inactiveColor
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scaleAndColor.first
                            scaleY = scaleAndColor.first
                            alpha =
                                ((scaleAndColor.first - 0.8f) / 0.4f * 0.4f + 0.6f).coerceIn(0.6f, 1f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d", value),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = scaleAndColor.second
                        )
                    )
                }
            }
        }
    }
}
