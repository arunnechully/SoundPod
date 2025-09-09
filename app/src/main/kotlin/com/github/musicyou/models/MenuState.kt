package com.github.musicyou.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
class MenuState {
    var isDisplayed by mutableStateOf(false)
        private set

    var content by mutableStateOf<@Composable () -> Unit>({})
        private set

    fun display(content: @Composable () -> Unit) {
        this.content = content
        isDisplayed = true
    }

    fun hide() {
        isDisplayed = false
    }
}

val LocalMenuState = staticCompositionLocalOf { MenuState() }