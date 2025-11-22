@file:Suppress("DEPRECATION")

package com.github.soundpod.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.github.core.ui.LocalAppearance

@Composable
fun ThemedLottieBackground(
    animationNumber: Int,
    modifier: Modifier = Modifier
) {
    val (colorPalette) = LocalAppearance.current

    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.Asset("lottie/bg$animationNumber.lottie")
    )

    val progress = animateLottieCompositionAsState(
        composition = compositionResult.value,
        iterations = LottieConstants.IterateForever
    )

    compositionResult.value?.let { composition ->
        LottieAnimation(
            composition = composition,
            progress = { progress.value },
            modifier = modifier
                .background(colorPalette.background3)
                .alpha(0.5f),
            contentScale = ContentScale.Crop
        )
    }
}
