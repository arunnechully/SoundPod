package com.github.soundpod.ui.appearance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun LikeAnimation(
    isLiked: Boolean,
    modifier: Modifier = Modifier,
    animationSpeed: Float = 1.5f
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/RedLike.lottie"))
    val lottieAnimatable = rememberLottieAnimatable()

    var isInitialLoad by remember { mutableStateOf(true) }

    LaunchedEffect(isLiked, composition) {
        val comp = composition ?: return@LaunchedEffect

        if (isInitialLoad) {
            lottieAnimatable.snapTo(comp, progress = if (isLiked) 1f else 0f)
            isInitialLoad = false
        } else {
            if (isLiked) {
                lottieAnimatable.animate(
                    composition = comp,
                    speed = animationSpeed
                )
            }
        }
    }

    LottieAnimation(
        composition = composition,
        progress = { lottieAnimatable.progress },
        modifier = modifier
    )
}