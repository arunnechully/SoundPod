package com.github.soundpod.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    isPlaying: Boolean = true
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottie/loading_animation.lottie")
    )

    LottieAnimation(
        composition = composition,
        // Loop forever by default
        iterations = LottieConstants.IterateForever,
        isPlaying = isPlaying,
        speed = speed,
        modifier = modifier
    )
}