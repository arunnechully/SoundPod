package com.github.soundpod.ui.common

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun WidgetCustomizationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // UI State
    var isWhiteSelected by remember { mutableStateOf(false) }
    var opacity by remember { mutableFloatStateOf(0.5f) }
    var matchDarkMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isWhiteSelected && !matchDarkMode) Color.White.copy(alpha = opacity)
                        else Color.Black.copy(alpha = opacity)
                    )
            ) {
                Text("Widget Preview", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Background color", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isWhiteSelected,
                        onClick = { isWhiteSelected = true },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFAECBFA), unselectedColor = Color.Gray)
                    )
                    Text("White", color = Color.White)
                }

                HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isWhiteSelected,
                        onClick = { isWhiteSelected = false },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFAECBFA), unselectedColor = Color.Gray)
                    )
                    Text("Black", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${(opacity * 100).toInt()}%", color = Color.White, modifier = Modifier.width(48.dp))
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFAECBFA),
                            activeTrackColor = Color.Gray,
                            inactiveTrackColor = Color.DarkGray
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Match with Dark mode", color = Color.White)
                Switch(
                    checked = matchDarkMode,
                    onCheckedChange = { matchDarkMode = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFAECBFA), checkedTrackColor = Color(0xFF3F4E6D))
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { onNavigateBack() }) {
                Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        val syncIntent = Intent("com.github.soundpod.SYNC_WIDGET").apply {
                            setPackage(context.packageName)
                        }
                        context.sendBroadcast(syncIntent)
                        onNavigateBack()
                    }
                }
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}