package com.github.soundpod.ui.screens.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ActionChipItem(
    val id: String,
    @DrawableRes val icon: Int,
    val label: String? = null,
    val isSelected: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun PlayerTabs(
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    isDisliked: Boolean,
    likeCount: String? = null,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    extraActions: List<ActionChipItem>,
    pillBackgroundColor: Color = Color.White.copy(alpha = 0.12f),
    selectedPillBackgroundColor: Color = Color.White.copy(alpha = 0.24f),
    contentColor: Color = Color.White,
    dividerColor: Color = Color.White.copy(alpha = 0.2f),
    @DrawableRes likeIcon: Int,
    @DrawableRes likeFilledIcon: Int = likeIcon,
    @DrawableRes dislikeIcon: Int,
    @DrawableRes dislikeFilledIcon: Int = dislikeIcon,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item(key = "like_dislike_pill") {
            LikeDislikePill(
                isLiked = isLiked,
                isDisliked = isDisliked,
                likeCount = likeCount,
                onLikeClick = onLikeClick,
                onDislikeClick = onDislikeClick,
                backgroundColor = pillBackgroundColor,
                contentColor = contentColor,
                dividerColor = dividerColor,
                likeIcon = likeIcon,
                likeFilledIcon = likeFilledIcon,
                dislikeIcon = dislikeIcon,
                dislikeFilledIcon = dislikeFilledIcon
            )
        }

        items(extraActions, key = { it.id }) { action ->
            SingleActionPill(
                item = action,
                backgroundColor = if (action.isSelected) selectedPillBackgroundColor else pillBackgroundColor,
                contentColor = contentColor
            )
        }
    }
}

@Composable
private fun LikeDislikePill(
    isLiked: Boolean,
    isDisliked: Boolean,
    likeCount: String?,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    dividerColor: Color,
    @DrawableRes likeIcon: Int,
    @DrawableRes likeFilledIcon: Int,
    @DrawableRes dislikeIcon: Int,
    @DrawableRes dislikeFilledIcon: Int,
) {
    val likeInteraction = remember { MutableInteractionSource() }
    val dislikeInteraction = remember { MutableInteractionSource() }

    val isLikePressed by likeInteraction.collectIsPressedAsState()
    val isDislikePressed by dislikeInteraction.collectIsPressedAsState()

    val containerScale by animateFloatAsState(
        targetValue = if (isLikePressed || isDislikePressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "likeDislikeContainerScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxHeight()
                .clickable(
                    interactionSource = likeInteraction,
                    indication = ripple(color = contentColor),
                    onClick = onLikeClick
                )
                .padding(start = 14.dp, end = 12.dp)
        ) {
            Icon(
                painter = painterResource(id = if (isLiked) likeFilledIcon else likeIcon),
                contentDescription = "Like",
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            if (!likeCount.isNullOrBlank()) {
                Text(
                    text = likeCount,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(dividerColor)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .clickable(
                    interactionSource = dislikeInteraction,
                    indication = ripple(color = contentColor),
                    onClick = onDislikeClick
                )
                .padding(start = 12.dp, end = 14.dp)
        ) {
            Icon(
                painter = painterResource(id = if (isDisliked) dislikeFilledIcon else dislikeIcon),
                contentDescription = "Dislike",
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SingleActionPill(
    item: ActionChipItem,
    backgroundColor: Color,
    contentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .bounceClick(pressedScale = 0.94f, rippleColor = contentColor, onClick = item.onClick)
            .height(38.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            painter = painterResource(id = item.icon),
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        if (!item.label.isNullOrBlank()) {
            Text(
                text = item.label,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}

fun Modifier.bounceClick(
    pressedScale: Float = 0.94f,
    rippleColor: Color = Color.Unspecified,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(CircleShape)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = rippleColor),
            onClick = onClick
        )
}