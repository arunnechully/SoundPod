package com.soundpod.music.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchBar(
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val isDarkTheme = isSystemInDarkTheme()
    val iconColor = if (isDarkTheme) Color.White else Color.Black
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = iconColor,
                    modifier = Modifier
                        .size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            // Text Field (with custom placeholder)
            Box(
                modifier = Modifier
                    .weight(0.5f) // As specified in your prompt
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },

                    // Placeholder: Text shown when the field is empty
                    placeholder = {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 22.sp
                            )
                        )
                    },

                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = iconColor,
                        focusedContainerColor =  Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),

                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Trailing icons
            IconButton(
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice",
                    tint = iconColor

                )
            }
        }
    }
}