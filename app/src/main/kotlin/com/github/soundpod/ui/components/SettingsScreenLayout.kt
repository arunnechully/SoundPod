package com.github.soundpod.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenLayout(
    title: String,
    description: String? = null,
    onBackClick: () -> Unit,
    scrollable: Boolean = true,
    horizontalPadding: Dp = 14.dp,
    actions: @Composable RowScope.() -> Unit = {},
    dropDownMenuContent: @Composable (ColumnScope.(dismissMenu: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val (colorPalette) = LocalAppearance.current

    SettingsScreenLayout(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.titleSmall,
                        color = colorPalette.text.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        onBackClick = onBackClick,
        scrollable = scrollable,
        horizontalPadding = horizontalPadding,
        actions = actions,
        dropDownMenuContent = dropDownMenuContent,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenLayout(
    title: @Composable (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    scrollable: Boolean = true,
    horizontalPadding: Dp = 14.dp,
    actions: @Composable RowScope.() -> Unit = {},
    dropDownMenuContent: @Composable (ColumnScope.(dismissMenu: () -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val (colorPalette) = LocalAppearance.current
    val scrollState = rememberScrollState()
    var showDropDown by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { title?.invoke() },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp),
                                tint = colorPalette.text
                            )
                        }
                    }
                },
                actions = {
                    actions()
                    if (dropDownMenuContent != null) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = { showDropDown = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = colorPalette.text,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            CustomDropdownMenu(
                                expanded = showDropDown,
                                onDismissRequest = { showDropDown = false }
                            ) {
                                dropDownMenuContent { showDropDown = false }
                            }
                        }
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
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = horizontalPadding)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
        ) {
            content()
        }
    }
}