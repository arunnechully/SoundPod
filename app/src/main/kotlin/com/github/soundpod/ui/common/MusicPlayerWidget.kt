package com.github.soundpod.ui.common

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.queueLoopEnabledKey
import com.github.soundpod.utils.trackLoopEnabledKey
import java.io.File

val widgetSongTitleKey = stringPreferencesKey("widget_song_title")
val widgetArtistKey = stringPreferencesKey("widget_artist")
val widgetIsPlayingKey = booleanPreferencesKey("widget_is_playing")
val widgetArtworkPathKey = stringPreferencesKey("widget_artwork_path")
val isPlayingParamKey = ActionParameters.Key<Boolean>("is_playing_param")

class MusicPlayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val songTitle = prefs[widgetSongTitleKey] ?: "Not Playing"
                val artist = prefs[widgetArtistKey] ?: "SoundPod"
                val isPlaying = prefs[widgetIsPlayingKey] ?: false

                val artworkPath = prefs[widgetArtworkPathKey]
                val bitmap = artworkPath?.let { path ->
                    try {
                        if (File(path).exists()) BitmapFactory.decodeFile(path) else null
                    } catch (_: Exception) {
                        null
                    }
                }

                WidgetContent(
                    songTitle = songTitle,
                    artist = artist,
                    isPlaying = isPlaying,
                    bitmap = bitmap
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        songTitle: String,
        artist: String,
        isPlaying: Boolean,
        bitmap: Bitmap?
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface) // Cleaner background
                .cornerRadius(24.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HEADER (Album Art + Info)
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val imageProvider = if (bitmap != null) ImageProvider(bitmap) else ImageProvider(R.drawable.music_icon)

                Image(
                    provider = imageProvider,
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.size(72.dp).cornerRadius(16.dp)
                )

                Spacer(modifier = GlanceModifier.width(16.dp))

                // Text wrapped in a weight modifier to prevent overflow
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = songTitle,
                        maxLines = 1, // Prevents layout breaking
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = artist,
                        maxLines = 1, // Prevents layout breaking
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(20.dp))

            // CONTROL BUTTONS
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularControlButton(
                    iconRes = R.drawable.shuffle,
                    description = "Shuffle",
                    iconSize = 22.dp, touchSize = 40.dp,
                    tint = GlanceTheme.colors.onSurfaceVariant,
                    actionModifier = GlanceModifier.clickable(actionRunCallback<ToggleShuffleAction>())
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                CircularControlButton(
                    iconRes = R.drawable.skip_previous,
                    description = "Previous",
                    iconSize = 28.dp, touchSize = 48.dp,
                    tint = GlanceTheme.colors.onSurface,
                    actionModifier = GlanceModifier.clickable(actionRunCallback<SkipPreviousAction>())
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Prominent Play/Pause Button
                ProminentPlayPauseButton(
                    isPlaying = isPlaying,
                    actionModifier = GlanceModifier.clickable(
                        actionRunCallback<TogglePlayAction>(actionParametersOf(isPlayingParamKey to isPlaying))
                    )
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                CircularControlButton(
                    iconRes = R.drawable.skip_next,
                    description = "Next",
                    iconSize = 28.dp, touchSize = 48.dp,
                    tint = GlanceTheme.colors.onSurface,
                    actionModifier = GlanceModifier.clickable(actionRunCallback<SkipNextAction>())
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                CircularControlButton(
                    iconRes = R.drawable.repeat_off,
                    description = "Repeat",
                    iconSize = 22.dp, touchSize = 40.dp,
                    tint = GlanceTheme.colors.onSurfaceVariant,
                    actionModifier = GlanceModifier.clickable(actionRunCallback<ToggleRepeatAction>())
                )
            }
        }
    }
}

// Standard invisible touch target button
@Composable
private fun CircularControlButton(
    iconRes: Int, description: String, iconSize: Dp, touchSize: Dp, tint: ColorProvider, actionModifier: GlanceModifier
) {
    Box(
        modifier = actionModifier.size(touchSize).cornerRadius(touchSize / 2),
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

// Emphasized filled button for Play/Pause (Similar to Samsung/Spotify)
@Composable
private fun ProminentPlayPauseButton(
    isPlaying: Boolean,
    actionModifier: GlanceModifier
) {
    Box(
        modifier = actionModifier
            .size(56.dp)
            .background(GlanceTheme.colors.primary)
            .cornerRadius(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(if (isPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = "Play/Pause",
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(32.dp)
        )
    }
}

class TogglePlayAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val isPlaying = parameters[isPlayingParamKey] ?: false
        val actionString = if (isPlaying) "com.github.soundpod.pause" else "com.github.soundpod.play"
        context.sendBroadcast(Intent(actionString).setPackage(context.packageName))
    }
}

class SkipNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.sendBroadcast(Intent("com.github.soundpod.next").setPackage(context.packageName))
    }
}

class SkipPreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.sendBroadcast(Intent("com.github.soundpod.previous").setPackage(context.packageName))
    }
}

class ToggleRepeatAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = context.preferences
        val trackLoop = prefs.getBoolean(trackLoopEnabledKey, false)
        val queueLoop = prefs.getBoolean(queueLoopEnabledKey, false)

        prefs.edit {
            when {
                !trackLoop && !queueLoop -> putBoolean(queueLoopEnabledKey, true)
                queueLoop -> {
                    putBoolean(queueLoopEnabledKey, false)
                    putBoolean(trackLoopEnabledKey, true)
                }

                else -> putBoolean(trackLoopEnabledKey, false)
            }
        }
    }
}

class ToggleShuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Implement shuffle preference toggle here when added to PlayerService
    }
}