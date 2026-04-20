package com.github.soundpod.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.ui.screens.player.CustomAnimatedVisibility
import kotlinx.coroutines.delay

@Composable
fun PlaylistHeader(
    isEditMode: Boolean,
    isAllSelected: Boolean,
    onSelectAllToggle: () -> Unit,
    onClearQueue: () -> Unit
) {
    val ctx = LocalContext.current
    val (colorPalette) = LocalAppearance.current

    var isClearExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) isClearExpanded = false
    }

    LaunchedEffect(isClearExpanded) {
        if (isClearExpanded) {
            delay(3000)
            isClearExpanded = false
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isAllSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (isAllSelected) MaterialTheme.colorScheme.primary else colorPalette.text.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onSelectAllToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    CustomAnimatedVisibility(
                        visible = isAllSelected,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Select All",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colorPalette.glass)
                    .clickable {
                        Toast.makeText(ctx, "Function not setup yet", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add),
                    contentDescription = "add",
                    tint = colorPalette.text,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = isEditMode,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
            ) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .defaultMinSize(minWidth = 32.dp)
                        .clip(CircleShape)
                        .background(colorPalette.glass)
                        .clickable {
                            if (!isClearExpanded) {
                                isClearExpanded = true
                            } else {
                                onClearQueue()
                                isClearExpanded = false
                            }
                        }
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = 150f
                            )
                        )
                        .padding(horizontal = if (isClearExpanded) 14.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isClearExpanded,
                        transitionSpec = {
                            fadeIn(
                                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                            ) togetherWith fadeOut(
                                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                            )
                        },
                        label = "clear_button_anim"
                    ) { expanded ->
                        if (expanded) {
                            Text(
                                text = "Clear",
                                color = colorPalette.text,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Queue",
                                tint = colorPalette.text,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            thickness = 0.5.dp,
            color = colorPalette.text.copy(alpha = 0.1f)
        )
    }
}