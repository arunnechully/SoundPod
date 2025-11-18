package com.github.soundpod.ui.screens.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.core.ui.utils.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

@Stable
@JvmInline
value class SwipeState @PublishedApi internal constructor(
    private val offsetLazy: Lazy<Animatable<Float, AnimationVector1D>> = lazy { acquire() }
) {
    internal val offset get() = offsetLazy.value

    private companion object {
        private val animatables = mutableListOf<Animatable<Float, AnimationVector1D>>()
        private val coroutineScope = CoroutineScope(Dispatchers.IO)

        fun acquire() = animatables.removeFirstOrNull() ?: Animatable(0f)
        fun recycle(animatable: Animatable<Float, AnimationVector1D>) {
            coroutineScope.launch {
                animatable.snapTo(0f)
                animatables += animatable
            }
        }
    }

    @Composable
    fun calculateOffset(bounds: ClosedRange<Dp>? = null) =
        offset.value.px.dp.let { if (bounds == null) it else it.coerceIn(bounds) }

    @PublishedApi
    internal fun recycle() = recycle(offset)
}

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun rememberSwipeState(key: Any?): SwipeState {
    val state = remember(key) { SwipeState() }

    DisposableEffect(key) {
        onDispose {
            state.recycle()
        }
    }

    return state
}

fun Modifier.onSwipe(
    state: SwipeState? = null,
    key: Any = Unit,
    animateOffset: Boolean = false,
    orientation: Orientation = Orientation.Horizontal,
    delay: Duration = Duration.ZERO,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) },
    animationSpec: AnimationSpec<Float> = spring(),
    bounds: ClosedRange<Dp>? = null,
    requireUnconsumed: Boolean = false,
    onSwipeOut: suspend (animationJob: Job) -> Unit
) = onSwipe(
    state = state,
    key = key,
    animateOffset = animateOffset,
    onSwipeLeft = onSwipeOut,
    onSwipeRight = onSwipeOut,
    orientation = orientation,
    delay = delay,
    decay = decay,
    animationSpec = animationSpec,
    requireUnconsumed = requireUnconsumed,
    bounds = bounds
)

@Suppress("CyclomaticComplexMethod")
fun Modifier.onSwipe(
    state: SwipeState? = null,
    key: Any = Unit,
    animateOffset: Boolean = false,
    onSwipeLeft: suspend (animationJob: Job) -> Unit = { },
    onSwipeRight: suspend (animationJob: Job) -> Unit = { },
    orientation: Orientation = Orientation.Horizontal,
    delay: Duration = Duration.ZERO,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) },
    animationSpec: AnimationSpec<Float> = spring(),
    bounds: ClosedRange<Dp>? = null,
    requireUnconsumed: Boolean = false
) = this.composed {
    val swipeState = state ?: rememberSwipeState(key)

    pointerInput(key) {
        coroutineScope {
            val velocityTracker = VelocityTracker()

            // fling loop, doesn't really offset anything but simulates the animation beforehand
            while (isActive) {
                velocityTracker.resetTracking()

                awaitPointerEventScope {
                    val pointer = awaitFirstDown(requireUnconsumed = requireUnconsumed).id
                    launch { swipeState.offset.snapTo(0f) }

                    val onDrag: (PointerInputChange) -> Unit = {
                        val change =
                            if (orientation == Orientation.Horizontal) it.positionChange().x
                            else it.positionChange().y

                        launch { swipeState.offset.snapTo(swipeState.offset.value + change) }

                        velocityTracker.addPosition(it.uptimeMillis, it.position)
                        if (change != 0f) it.consume()
                    }

                    if (orientation == Orientation.Horizontal) {
                        awaitHorizontalTouchSlopOrCancellation(pointer) { change, _ -> onDrag(change) }
                            ?: return@awaitPointerEventScope
                        horizontalDrag(pointer, onDrag)
                    } else {
                        awaitVerticalTouchSlopOrCancellation(pointer) { change, _ -> onDrag(change) }
                            ?: return@awaitPointerEventScope
                        verticalDrag(pointer, onDrag)
                    }
                }

                // drag completed, calculate velocity
                val targetOffset = decay().calculateTargetValue(
                    initialValue = swipeState.offset.value,
                    initialVelocity = velocityTracker.calculateVelocity()
                        .let { if (orientation == Orientation.Horizontal) it.x else it.y }
                )
                val size = if (orientation == Orientation.Horizontal) size.width else size.height

                launch animationEnd@{
                    when {
                        targetOffset >= size / 2 -> {
                            val animationJob = launch {
                                swipeState.offset.animateTo(
                                    targetValue = size.toFloat(),
                                    animationSpec = animationSpec
                                )
                            }
                            delay(delay)
                            onSwipeRight(animationJob)
                        }

                        targetOffset <= -size / 2 -> {
                            val animationJob = launch {
                                swipeState.offset.animateTo(
                                    targetValue = -size.toFloat(),
                                    animationSpec = animationSpec
                                )
                            }
                            delay(delay)
                            onSwipeLeft(animationJob)
                        }
                    }
                    swipeState.offset.animateTo(
                        targetValue = 0f,
                        animationSpec = animationSpec
                    )
                }
            }
        }
    }.let { modifier ->
        when {
            animateOffset && orientation == Orientation.Horizontal ->
                modifier.offset(x = swipeState.calculateOffset(bounds = bounds))

            animateOffset && orientation == Orientation.Vertical ->
                modifier.offset(y = swipeState.calculateOffset(bounds = bounds))

            else -> modifier
        }
    }
}

fun Modifier.swipeToClose(
    key: Any = Unit,
    state: SwipeState? = null,
    delay: Duration = Duration.ZERO,
    decay: Density.() -> DecayAnimationSpec<Float> = { splineBasedDecay(this) },
    requireUnconsumed: Boolean = false,
    onClose: suspend (animationJob: Job) -> Unit
) = this.composed {
    val swipeState = state ?: rememberSwipeState(key)

    val density = LocalDensity.current

    var currentWidth by remember { mutableIntStateOf(0) }
    val currentWidthDp by remember { derivedStateOf { currentWidth.px.dp(density) } }
    val bounds by remember { derivedStateOf { -currentWidthDp..0.dp } }

    this
        .onSizeChanged { currentWidth = it.width }
        .alpha((currentWidthDp + swipeState.calculateOffset(bounds = bounds)) / currentWidthDp)
        .onSwipe(
            state = swipeState,
            key = key,
            animateOffset = true,
            onSwipeLeft = onClose,
            orientation = Orientation.Horizontal,
            delay = delay,
            decay = decay,
            requireUnconsumed = requireUnconsumed,
            bounds = bounds
        )
}