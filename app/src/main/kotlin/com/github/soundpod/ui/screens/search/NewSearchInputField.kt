package com.github.soundpod.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R

@Composable
fun NewSearchInputField(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    pop: () -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = pop) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colorPalette.text
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (textFieldState.text.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.search),
                    style = TextStyle(
                        fontSize = 20.sp,
                        color = colorPalette.textSecondary,
                        fontWeight = FontWeight.Normal
                    )
                )
            }

            BasicTextField(
                state = textFieldState,
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    color = colorPalette.text,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(colorPalette.accent),
                lineLimits = androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = {
                    onSearch(textFieldState.text.toString())
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (textFieldState.text.isNotEmpty()) {
            IconButton(onClick = { textFieldState.clearText() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = colorPalette.text
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}