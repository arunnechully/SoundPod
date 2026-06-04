package com.github.soundpod.ui.screens.settings

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.ui.appearance.BackgroundStyles
import com.github.soundpod.ui.appearance.PLAYER_BACKGROUND_STYLE_KEY
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.rememberPreference

// Helper data class to clean up preset listing
private data class BackgroundPreset(val title: String, val asset: String, val styleId: Int)

@Composable
fun BackgroundSettingsContent() {
    val context = LocalContext.current
    val prefs = context.preferences

    // State
    val currentStyle by rememberPreference(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.MORPHING)

    val presetBaseName = stringResource(id = R.string.preset)

    val presets = listOf(
        BackgroundPreset("$presetBaseName 1", "lottie/bg1.lottie", BackgroundStyles.ABSTRACT_1),
        BackgroundPreset("$presetBaseName 2", "lottie/bg2.lottie", BackgroundStyles.ABSTRACT_2),
        BackgroundPreset("$presetBaseName 3", "lottie/bg3.lottie", BackgroundStyles.ABSTRACT_3),
        BackgroundPreset("$presetBaseName 4", "lottie/bg4.lottie", BackgroundStyles.ABSTRACT_4)
    )

    Column {

        SettingsGroup(
            title = stringResource(id = R.string.style)
        ) {
            BackgroundOptionItem(
                title = stringResource(id = R.string.dynamic_colors),
                description = stringResource(id = R.string.dynamic_colors_discription),
                selected = (currentStyle == BackgroundStyles.MORPHING || currentStyle == 0),
                onClick = { prefs.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, BackgroundStyles.MORPHING) } }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionHeader(
            stringResource(id = R.string.presets)
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            presets.chunked(2).forEach { rowPresets ->
                TwoColumnRow {
                    rowPresets.forEach { preset ->
                        BackgroundPreviewCard(
                            preset = preset,
                            selected = currentStyle == preset.styleId,
                            onClick = { prefs.edit { putInt(PLAYER_BACKGROUND_STYLE_KEY, preset.styleId) } }
                        )
                    }
                    if (rowPresets.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    val (colorPalette) = LocalAppearance.current
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = colorPalette.text,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
    )
}

@Composable
private fun RowScope.BackgroundPreviewCard(
    preset: BackgroundPreset,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset(preset.asset))

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
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
            compositionResult.value?.let { composition ->
                LottieAnimation(
                    composition = composition,
                    progress = { 0.5f },
                    modifier = Modifier.fillMaxSize().alpha(0.7f),
                    contentScale = ContentScale.Crop
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .background(colorPalette.accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = colorPalette.onAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = preset.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) colorPalette.accent else colorPalette.text,
            maxLines = 1
        )
    }
}

@Composable
fun TwoColumnRow(content: @Composable RowScope.() -> Unit) {
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
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colorPalette.text)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = colorPalette.textSecondary)
        }
    }
}
