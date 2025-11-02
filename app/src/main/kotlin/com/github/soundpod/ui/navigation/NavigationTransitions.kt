package com.github.soundpod.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavDestination
import com.github.soundpod.utils.areTopDestinations
import com.github.soundpod.utils.isTopDestination
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import soup.compose.material.motion.animation.materialSharedAxisXIn
import soup.compose.material.motion.animation.materialSharedAxisXOut

object NavigationTransitions {
    fun enterTransition(
        targetDestination: NavDestination,
        slideDistance: Int
    ): EnterTransition {
        return if (targetDestination.isTopDestination()) materialFadeThroughIn()
        else materialSharedAxisXIn(
            forward = true,
            slideDistance = slideDistance
        )
    }

    fun exitTransition(
        targetDestination: NavDestination,
        slideDistance: Int
    ): ExitTransition {
        return if (targetDestination.isTopDestination()) materialFadeThroughOut()
        else materialSharedAxisXOut(
            forward = true,
            slideDistance = slideDistance
        )
    }

    fun popEnterTransition(
        initialDestination: NavDestination,
        targetDestination: NavDestination,
        slideDistance: Int
    ): EnterTransition {
        return if (listOf(initialDestination, targetDestination).areTopDestinations()) {
            materialFadeThroughIn()
        } else materialSharedAxisXIn(
            forward = false,
            slideDistance = slideDistance
        )
    }

    fun popExitTransition(
        initialDestination: NavDestination,
        targetDestination: NavDestination,
        slideDistance: Int
    ): ExitTransition {
        return if (listOf(initialDestination, targetDestination).areTopDestinations()) {
            materialFadeThroughOut()
        } else materialSharedAxisXOut(
            forward = false,
            slideDistance = slideDistance
        )
    }
}