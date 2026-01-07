package com.github.soundpod.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp // Import Dp
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenLayout(
    title: String,
    onBackClick: () -> Unit,
    scrollable: Boolean = true,
    horizontalPadding: Dp = 14.dp,
    content: @Composable () -> Unit
) {
    SettingsScreenLayout(
        title = { Text(title) },
        onBackClick = onBackClick,
        scrollable = scrollable,
        horizontalPadding = horizontalPadding,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenLayout(
    title: @Composable () -> Unit,
    onBackClick: () -> Unit,
    scrollable: Boolean = true,
    horizontalPadding: Dp = 14.dp,
    content: @Composable () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp),
                            tint = colorPalette.text
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = colorPalette.text
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = horizontalPadding)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
        ) {
            content()
        }
    }
}