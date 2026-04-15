package com.github.core.ui

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.github.core.ui.utils.DpParceler
import com.github.core.ui.utils.roundedShape
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

@Parcelize
@TypeParceler<Dp, DpParceler>
@Immutable
data class Appearance(
    val colorPalette: ColorPalette,
    val typography: Typography,
    val thumbnailShapeCorners: Dp
) : Parcelable {
    @IgnoredOnParcel
    val thumbnailShape = thumbnailShapeCorners.roundedShape

    operator fun component4() = thumbnailShape
}

val LocalAppearance = staticCompositionLocalOf<Appearance> { error("No appearance provided") }

@Composable
inline fun rememberAppearance(
    vararg keys: Any = arrayOf(Unit),
    isDark: Boolean = isSystemInDarkTheme(),
    crossinline provide: (isSystemInDarkTheme: Boolean) -> Appearance
) = remember(keys, isDark) {
    mutableStateOf(provide(isDark))
}

@Composable
fun appearance(
    source: ColorSource,
    mode: ColorMode,
    darkness: Darkness,
    materialAccentColor: Color?,
    sampleBitmap: Bitmap?,
    fontFamily: BuiltInFontFamily,
    applyFontPadding: Boolean,
    thumbnailRoundness: Dp,
    isSystemInDarkTheme: Boolean = isSystemInDarkTheme()
): Appearance {
    val isDark = remember(mode, isSystemInDarkTheme) {
        mode == ColorMode.Dark || (mode == ColorMode.System && isSystemInDarkTheme)
    }

    val colorPalette = remember(
        source,
        darkness,
        isDark,
        materialAccentColor,
        sampleBitmap
    ) {
        colorPaletteOf(
            source = source,
            darkness = darkness,
            isDark = isDark,
            materialAccentColor = materialAccentColor,
            sampleBitmap = sampleBitmap
        )
    }

    return rememberAppearance(
        colorPalette,
        fontFamily,
        applyFontPadding,
        thumbnailRoundness,
        isDark = isDark
    ) {
        Appearance(
            colorPalette = colorPalette,
            typography = typographyOf(
                color = colorPalette.text,
                fontFamily = fontFamily,
                applyFontPadding = applyFontPadding
            ),
            thumbnailShapeCorners = thumbnailRoundness
        )
    }.value
}