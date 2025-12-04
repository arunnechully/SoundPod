package com.github.soundpod.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R

@Composable
fun NewSearchBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {},
    onMicClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val (colorPalette) = LocalAppearance.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { onBackClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = colorPalette.text,
                    modifier = Modifier
                        .size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Box(
                modifier = Modifier
                    .weight(0.5f)
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholderText,
                        color = colorPalette.text.copy(alpha = 0.5f),
                        style = typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colorPalette.text,
                            fontSize = 18.sp
                        )
                    )
                }


                BasicTextField(
                    value = text,
                    onValueChange = {
                        onTextChange(it)
                    },
                    singleLine = true,
                    textStyle = typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = colorPalette.text,
                        fontSize = 18.sp
                    ),
                    cursorBrush = SolidColor(colorPalette.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (text.isNotBlank()) {
                                onSearch(text)
                            }
                        }
                    )
                )
            }

            // Trailing icons
            if (text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = colorPalette.text,
                    )
                }
            } else {
                IconButton(onClick = onMicClick) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Voice",
                        tint = colorPalette.text,

                    )
                }
            }
        }
    }
}