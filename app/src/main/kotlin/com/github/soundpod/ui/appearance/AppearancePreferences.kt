//package com.github.soundpod.ui.appearance
//
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.State
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.graphics.Color
//import com.github.core.ui.BuiltInFontFamily
//import com.github.core.ui.BuiltInFontFamily.Poppins
//import com.github.core.ui.ColorMode
//import com.github.core.ui.ColorSource
//import com.github.core.ui.Darkness
//import com.github.core.ui.ThumbnailRoundness
//
//object AppearancePreferences : GlobalPreferencesHolder() {
//    // Theme state for UI updates
//    private val themeStateCounter = mutableIntStateOf(0)
//
//    //Player Background
//    var playerBackgroundEnabled by boolean(false)
//    var backgroundAnimation by int(1)
//    var gradientAnimationEnabled by boolean(false)
//
//    // Property delegates with change notification
//    private var _colorSource by enum(
//        when (OldPreferences.oldColorPaletteName) {
//            ColorPaletteName.Default, ColorPaletteName.PureBlack -> ColorSource.Default
//            ColorPaletteName.Dynamic, ColorPaletteName.AMOLED -> ColorSource.Dynamic
//            ColorPaletteName.MaterialYou -> ColorSource.MaterialYou
//        }
//    )
//    private var _colorMode by enum(
//        when (OldPreferences.oldColorPaletteMode) {
//            ColorPaletteMode.Light -> ColorMode.Light
//            ColorPaletteMode.Dark -> ColorMode.Dark
//            ColorPaletteMode.System -> ColorMode.System
//        }
//    )
//    private var _darkness by enum(
//        when (OldPreferences.oldColorPaletteName) {
//            ColorPaletteName.Default, ColorPaletteName.Dynamic, ColorPaletteName.MaterialYou -> Darkness.Normal
//            ColorPaletteName.PureBlack -> Darkness.PureBlack
//            ColorPaletteName.AMOLED -> Darkness.AMOLED
//        }
//    )
//
//    // Properties with observable delegates that trigger UI updates
//    var colorSource: ColorSource
//        get() = _colorSource
//        set(value) {
//            if (_colorSource != value) {
//                _colorSource = value
//                themeStateCounter.intValue++
//            }
//        }
//
//    var colorMode: ColorMode
//        get() = _colorMode
//        set(value) {
//            if (_colorMode != value) {
//                _colorMode = value
//                themeStateCounter.intValue++
//            }
//        }
//
//    var darkness: Darkness
//        get() = _darkness
//        set(value) {
//            if (_darkness != value) {
//                _darkness = value
//                themeStateCounter.intValue++
//            }
//        }
//
//    // Other preferences
//    var thumbnailRoundness by enum(ThumbnailRoundness.Medium)
//    var fontFamily by enum(Poppins)
//    var applyFontPadding by boolean(false)
//    val isShowingThumbnailInLockscreenProperty = boolean(true)
//    var isShowingThumbnailInLockscreen by isShowingThumbnailInLockscreenProperty
//    var swipeToHideSong by boolean(false)
//    var swipeToHideSongConfirm by boolean(true)
//    var maxThumbnailSize by int(1920)
//    var hideExplicit by boolean(false)
//    var autoPip by boolean(false)
//    var openPlayer by boolean(false)
//
//    /**
//     * Creates a State object that will change whenever theme settings are updated
//     */
//    @Composable
//    fun rememberThemeState(): State<Int> {
//        return remember { themeStateCounter }
//    }
//
//    /**
//     * Determine if dark theme should be used based on current settings
//     */
//    @Composable
//    fun isInDarkTheme(): Boolean {
//        val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
//        val themeState = rememberThemeState()
//
//        return remember(colorMode, systemDarkTheme, themeState.value) {
//            when (colorMode) {
//                ColorMode.System -> systemDarkTheme
//                ColorMode.Light -> false
//                ColorMode.Dark -> true
//            }
//        }
//    }
//
//    /**
//     * Get text color based on current theme
//     */
//    @Composable
//    fun getTextColor(isDark: Boolean = isInDarkTheme()): Color {
//        return if (isDark) Color.White else Color.Black
//    }
//
//    /**
//     * Get background color based on current theme and darkness level
//     */
//    @Composable
//    fun getBackgroundColor(isDark: Boolean = isInDarkTheme()): Color {
//        return if (isDark) {
//            if (darkness == Darkness.AMOLED || darkness == Darkness.PureBlack) {
//                Color.Black
//            } else {
//                Color(0xFF121212)
//            }
//        } else {
//            Color(0xFFFAFAFA)
//        }
//    }
//
//    /**
//     * Get secondary text color based on current theme
//     */
//    @Composable
//    fun getSecondaryTextColor(isDark: Boolean = isInDarkTheme()): Color {
//        return if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
//    }
//
//    /**
//     * Get surface color for cards and content areas
//     */
//    @Composable
//    fun getSurfaceColor(isDark: Boolean = isInDarkTheme()): Color {
//        return if (isDark) {
//            if (darkness == Darkness.AMOLED || darkness == Darkness.PureBlack) {
//                Color(0xFF121212) // Dark but not pure black
//            } else {
//                Color(0xFF2A2A2A)
//            }
//        } else {
//            Color(0xFFF0F0F0)
//        }
//    }
//}