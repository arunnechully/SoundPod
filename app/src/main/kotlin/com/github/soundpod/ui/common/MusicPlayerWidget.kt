package com.github.soundpod.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.github.soundpod.MainActivity
import com.github.soundpod.R

class MusicPlayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val currentSongTitle = "Heart"
        val currentArtist = "Halal Nasheed"
        val progress = 0.2f
        val timeElapsed = "0:18"
        val timeRemaining = "-2:24"

        provideContent {
            GlanceTheme {
                WidgetContent(
                    songTitle = currentSongTitle,
                    artist = currentArtist,
                    progress = progress,
                    timeElapsed = timeElapsed,
                    timeRemaining = timeRemaining
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        songTitle: String,
        artist: String,
        progress: Float,
        timeElapsed: String,
        timeRemaining: String
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surfaceVariant)
                .cornerRadius(24.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.music_icon),
                        contentDescription = "Album Art",
                        modifier = GlanceModifier
                            .size(64.dp)
                            .cornerRadius(12.dp)
                    )

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    Column {
                        Text(
                            text = songTitle,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = artist,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(20.dp))

                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = timeElapsed,
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = timeRemaining,
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = GlanceModifier.fillMaxWidth(),
                        color = GlanceTheme.colors.primary,
                        backgroundColor = GlanceTheme.colors.secondaryContainer
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(20.dp))

            // CONTROL BUTTONS (Runs in Background)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularControlButton(
                    iconRes = R.drawable.shuffle,
                    description = "Shuffle",
                    iconSize = 24.dp,
                    touchSize = 34.dp, // Large, easy-to-hit touch target!
                    tint = GlanceTheme.colors.onSurface,
                    actionModifier = GlanceModifier.clickable(actionRunCallback<ToggleShuffleAction>())
                )
                Spacer(modifier = GlanceModifier.defaultWeight())

                CircularControlButton(
                    iconRes = R.drawable.skip_previous,
                    description = "Previous",
                    iconSize = 22.dp,
                    touchSize = 34.dp,
                    tint = GlanceTheme.colors.onSurface,
                    actionModifier = GlanceModifier.clickable(actionRunCallback<SkipPreviousAction>())
                )
                Spacer(modifier = GlanceModifier.defaultWeight())

                CircularControlButton(
                    iconRes = R.drawable.play,
                    description = "Play/Pause",
                    iconSize = 34.dp,
                    touchSize = 34.dp,
                    tint = GlanceTheme.colors.onSurface,
                    actionModifier = GlanceModifier.clickable(actionRunCallback<TogglePlayAction>())
                )
                Spacer(modifier = GlanceModifier.defaultWeight())

                CircularControlButton(
                    iconRes = R.drawable.skip_next,
                    description = "Next",
                    iconSize = 22.dp,
                    touchSize = 34.dp,
                    tint = GlanceTheme.colors.onSurface,
                    actionModifier = GlanceModifier.clickable(actionRunCallback<SkipNextAction>())
                )
                Spacer(modifier = GlanceModifier.defaultWeight())

                CircularControlButton(
                    iconRes = R.drawable.repeat_off,
                    description = "Repeat",
                    iconSize = 24.dp,
                    touchSize = 34.dp,
                    tint = GlanceTheme.colors.onSurface,
                    actionModifier = GlanceModifier.clickable(actionRunCallback<ToggleRepeatAction>())
                )
            }
        }
    }
}
@Composable
private fun CircularControlButton(
    iconRes: Int,
    description: String,
    iconSize: Dp,
    touchSize: Dp,
    tint: ColorProvider,
    actionModifier: GlanceModifier
) {
    Box(
        modifier = actionModifier
            .size(touchSize)
            .cornerRadius(touchSize / 2),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = description,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(iconSize)
        )
    }
}

class TogglePlayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // TODO: Send play/pause intent to your Media3 PlayerService
    }
}
class SkipNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {}
}
class SkipPreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {}
}
class ToggleShuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {}
}
class ToggleRepeatAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {}
}