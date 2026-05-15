package com.github.soundpod

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.soundpod.ui.common.WidgetCustomizationScreen
import com.github.soundpod.ui.styling.AppTheme // Adjust import if needed

class WidgetSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppTheme(darkTheme = true) {
                WidgetCustomizationScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}