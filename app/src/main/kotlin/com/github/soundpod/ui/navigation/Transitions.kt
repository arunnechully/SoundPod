package com.github.soundpod.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

object Transitions {
    private val OneUiEasing = CubicBezierEasing(0.22f, 0.25f, 0.0f, 1.0f)
    private const val DURATION = 400

    fun enter(): EnterTransition = slideInHorizontally(
        initialOffsetX = { (it * 1.1).toInt() },
        animationSpec = tween(DURATION, easing = OneUiEasing)
    ) + fadeIn(
        animationSpec = tween(200)
    )
    fun exit(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { (-it * 0.25).toInt() },
        animationSpec = tween(DURATION, easing = OneUiEasing)
    ) + fadeOut(
        animationSpec = tween(DURATION),
        targetAlpha = 0.8f 
    )

    fun popEnter(): EnterTransition = slideInHorizontally(
        initialOffsetX = { (-it * 0.25).toInt() },
        animationSpec = tween(DURATION, easing = OneUiEasing)
    ) + fadeIn(
        animationSpec = tween(DURATION),
        initialAlpha = 0.8f
    )

    fun popExit(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { (it * 1.1).toInt() },
        animationSpec = tween(DURATION, easing = OneUiEasing)
    ) + fadeOut(
        animationSpec = tween(200)
    )
}