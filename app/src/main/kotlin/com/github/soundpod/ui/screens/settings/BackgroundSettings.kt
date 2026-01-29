package com.github.soundpod.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import coil3.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.github.core.ui.LocalAppearance
import com.github.soundpod.ui.appearance.BackgroundStyles
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_CUSTOM_COLOR_1
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_CUSTOM_COLOR_2
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_CUSTOM_IMAGE_KEY
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_IS_ANIMATED
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_STYLE_KEY
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.rememberPreference

@Composable
fun BackgroundSettings(onBackClick: () -> Unit) {
    val (colorPalette) = LocalAppearance.current
    val context = LocalContext.current

    // State
    val currentStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC)
    val color1 by rememberPreference(PLAYER_BACKGROUND_CUSTOM_COLOR_1, -1) // -1 = Auto
    val color2 by rememberPreference(PLAYER_BACKGROUND_CUSTOM_COLOR_2, -1) // -1 = None (Solid)
    val isAnimated by rememberPreference(PLAYER_BACKGROUND_IS_ANIMATED, true)
    val customImagePath by rememberPreference(PLAYER_BACKGROUND_CUSTOM_IMAGE_KEY, "")

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.preferences.edit {
                putString(PLAYER_BACKGROUND_CUSTOM_IMAGE_KEY, it.toString())
                putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.CUSTOM_IMAGE)
            }
        }
    }

    SettingsScreenLayout(title = "Player Background", onBackClick = onBackClick) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION 1: STYLE SELECTION ---
        Text("Style", style = MaterialTheme.typography.titleMedium, color = colorPalette.text, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        SettingsCard {
            BackgroundOptionItem(
                title = "Dynamic Fusion",
                description = "Customizable colors & animation",
                selected = currentStyle == BackgroundStyles.DYNAMIC,
                onClick = { context.preferences.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.DYNAMIC) } }
            )
        }

        // --- SECTION 2: FUSION CUSTOMIZATION (Only if Dynamic is selected) ---
        AnimatedVisibility(visible = currentStyle == BackgroundStyles.DYNAMIC) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Customize Look", style = MaterialTheme.typography.titleMedium, color = colorPalette.text, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))

                SettingsCard {
                    // 1. Animation Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Animation", style = MaterialTheme.typography.bodyLarge, color = colorPalette.text)
                            Text(if(isAnimated) "Breathing effect enabled" else "Static background", style = MaterialTheme.typography.bodySmall, color = colorPalette.textSecondary)
                        }
                        Switch(
                            checked = isAnimated,
                            onCheckedChange = { checked -> context.preferences.edit { putBoolean(PLAYER_BACKGROUND_IS_ANIMATED, checked) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = colorPalette.accent, checkedTrackColor = colorPalette.accent.copy(alpha = 0.5f))
                        )
                    }

                    // 2. Primary Color Picker
                    Text("Primary Color (Core)", style = MaterialTheme.typography.bodyMedium, color = colorPalette.textSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    ColorSelectorRow(selectedColor = color1, onColorSelected = { context.preferences.edit { putInt(PLAYER_BACKGROUND_CUSTOM_COLOR_1, it) } })

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Secondary Color Picker (For Gradient)
                    Text("Secondary Color (Gradient)", style = MaterialTheme.typography.bodyMedium, color = colorPalette.textSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    // Note: We pass -1 as "None" option here
                    ColorSelectorRow(selectedColor = color2, allowNone = true, onColorSelected = { context.preferences.edit { putInt(PLAYER_BACKGROUND_CUSTOM_COLOR_2, it) } })

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 3: PRESETS ---
        Text("Presets & Media", style = MaterialTheme.typography.titleMedium, color = colorPalette.text, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TwoColumnRow {
                AddCustomBackgroundCard(
                    customImageUri = customImagePath, selected = currentStyle == BackgroundStyles.CUSTOM_IMAGE,
                    onClick = { if (customImagePath.isNotEmpty()) context.preferences.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.CUSTOM_IMAGE) } else mediaPicker.launch(arrayOf("image/*")) },
                    onAddClick = { mediaPicker.launch(arrayOf("image/*")) }
                )
                BackgroundPreviewCard("Midnight Aura", "lottie/bg1.lottie", currentStyle == BackgroundStyles.ABSTRACT_1) { context.preferences.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.ABSTRACT_1) } }
            }
            TwoColumnRow {
                BackgroundPreviewCard("Golden Haze", "lottie/bg2.lottie", currentStyle == BackgroundStyles.ABSTRACT_2) { context.preferences.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.ABSTRACT_2) } }
                BackgroundPreviewCard("Violet Dream", "lottie/bg3.lottie", currentStyle == BackgroundStyles.ABSTRACT_3) { context.preferences.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.ABSTRACT_3) } }
            }
            TwoColumnRow {
                BackgroundPreviewCard("Alpine Night", "lottie/bg4.lottie", currentStyle == BackgroundStyles.ABSTRACT_4) { context.preferences.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.ABSTRACT_4) } }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun ColorSelectorRow(selectedColor: Int, allowNone: Boolean = false, onColorSelected: (Int) -> Unit) {
    // Basic Palette
    val colors = listOf(
        if (allowNone) -1 else -1, // -1 is Auto for Primary, None for Secondary
        Color(0xFFEF5350).toArgb(), Color(0xFFFFA726).toArgb(), Color(0xFFFFEE58).toArgb(),
        Color(0xFF66BB6A).toArgb(), Color(0xFF42A5F5).toArgb(), Color(0xFFAB47BC).toArgb(),
        Color(0xFF8D6E63).toArgb(), Color(0xFFBDBDBD).toArgb()
    )

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        colors.forEach { colorInt ->
            val isSelected = selectedColor == colorInt
            val isSpecial = colorInt == -1

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSpecial && !allowNone -> Color.DarkGray // "Auto" icon bg
                            isSpecial && allowNone -> Color.Transparent // "None" icon bg
                            else -> Color(colorInt)
                        }
                    )
                    .border(
                        width = if (isSelected) 2.dp else if (isSpecial && allowNone) 1.dp else 0.dp,
                        color = if (isSelected) Color.White else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorInt) },
                contentAlignment = Alignment.Center
            ) {
                if (isSpecial) {
                    if (!allowNone) {
                        // "Auto" Icon (Magic wand style or just 'A')
                        Text("A", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    } else {
                        // "None" Icon (X)
                        Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
                if (isSelected && !isSpecial) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun RowScope.AddCustomBackgroundCard(
    customImageUri: String,
    selected: Boolean,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.6f)
                .clip(RoundedCornerShape(16.dp))
                .background(colorPalette.background3)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) colorPalette.accent else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            if (customImageUri.isNotEmpty()) {
                // Show user's image
                AsyncImage(
                    model = customImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.7f)
                )
            } else {
                // Show Placeholder Pattern
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray)
                )
            }

            // The "+" Button Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colorPalette.accent.copy(alpha = 0.8f))
                    .clickable(onClick = onAddClick), // Clicking plus opens picker directly
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = colorPalette.onAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (customImageUri.isEmpty()) "Add Custom" else "Custom Media",
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colorPalette.accent else colorPalette.text,
            maxLines = 1
        )
    }
}

// --- UPDATED PREVIEW CARD (STATIC) ---
@Composable
fun RowScope.BackgroundPreviewCard(
    title: String,
    lottieFile: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    // Lottie Loader (Just load, don't animate state)
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.Asset(lottieFile)
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.6f)
                .clip(RoundedCornerShape(16.dp))
                .background(colorPalette.background3)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) colorPalette.accent else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            // The Animation (Static Progress)
            compositionResult.value?.let { composition ->
                LottieAnimation(
                    composition = composition,
                    progress = { 0.5f }, // FIXED PROGRESS: Shows a static snapshot from middle of animation
                    modifier = Modifier.fillMaxSize().alpha(0.7f),
                    contentScale = ContentScale.Crop
                )
            }

            // Selection Checkmark Overlay
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .background(colorPalette.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colorPalette.onAccent
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colorPalette.accent else colorPalette.text,
            maxLines = 1
        )
    }
}

// Helper to layout items in 2 columns
@Composable
fun TwoColumnRow(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun BackgroundOptionItem(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colorPalette.text)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = colorPalette.textSecondary)
        }
    }
}