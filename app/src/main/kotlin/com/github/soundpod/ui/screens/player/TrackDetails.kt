package com.github.soundpod.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.core.ui.LocalAppearance
import com.github.soundpod.R
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.ui.components.SettingsScreenLayout


@Composable
fun TrackDetails(
    pop: () -> Unit,
) {
    val (colorPalette) = LocalAppearance.current
    SettingsScreenLayout(
        scrollable = false,
        horizontalPadding = 0.dp,
        title = {},
        onBackClick = { pop() },
        actions = {

        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

//         Centered Playlist Cover Placeholder
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorPalette.text.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(id = R.drawable.music_icon),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colorPalette.text
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

//         Centered Title
            Text(
                text = stringResource(id = R.string.offline),
                style = typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorPalette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

//         Artist name
            Text(
                text = "todo",
                style = typography.bodyMedium,
                color = colorPalette.text.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))
        SettingsCard(
            shape = RoundedCornerShape(
                topStart = 25.dp,
                topEnd = 25.dp
            )
        ) {
            //todo
        }
        }
    }
}