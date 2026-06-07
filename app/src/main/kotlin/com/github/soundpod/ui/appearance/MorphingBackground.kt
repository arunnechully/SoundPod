package com.github.soundpod.ui.appearance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.*

/**
 * A Lottie-based morphing background that uses colors extracted from the thumbnail.
 * This uses the custom Lottie animation provided in assets.
 */
@Composable
fun MorphingBackground(
    colors: ColorClusters?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/ThemableAnimation.json"))
    
    val speed by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(1000),
        label = "speed"
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = speed,
    )

    // Fallback to theme colors if no thumbnail colors are provided
    val c1 = colors?.c1 ?: MaterialTheme.colorScheme.primary
    val c2 = colors?.c2 ?: MaterialTheme.colorScheme.secondary
    val c3 = colors?.c3 ?: MaterialTheme.colorScheme.tertiary

    val targetC1 = remember(c1, isDark) { c1.adaptToTheme(isDark) }
    val targetC2 = remember(c2, isDark) { c2.adaptToTheme(isDark) }
    val targetC3 = remember(c3, isDark) { c3.adaptToTheme(isDark) }

    val finalC1 by animateColorAsState(targetC1, tween(2500), label = "c1")
    val finalC2 by animateColorAsState(targetC2, tween(2500), label = "c2")
    val finalC3 by animateColorAsState(targetC3, tween(2500), label = "c3")

    // Map extracted colors to Lottie layers
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = finalC1.toArgb(),
            keyPath = arrayOf("Pink Gradient", "Ellipse 1", "Fill 1"),
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = finalC2.toArgb(),
            keyPath = arrayOf("Green Gradient", "Ellipse 1", "Fill 1"),
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = finalC3.toArgb(),
            keyPath = arrayOf("Cyan Gradient", "Ellipse 1", "Fill 1"),
        ),
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                finalC3.copy(alpha = 0.2f)
                    .compositeOver(if (isDark) Color(0xFF05050A) else Color(0xFFFAFAFF))
            )
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            dynamicProperties = dynamicProperties,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.75f)
        )
    }
}
