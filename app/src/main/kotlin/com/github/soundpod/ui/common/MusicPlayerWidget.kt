package com.github.soundpod.ui.common

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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
import java.io.File

val widgetSongTitleKey = stringPreferencesKey("widget_song_title")
val widgetArtistKey = stringPreferencesKey("widget_artist")
val widgetIsPlayingKey = booleanPreferencesKey("widget_is_playing")
val widgetArtworkPathKey = stringPreferencesKey("widget_artwork_path")
val isPlayingParamKey = ActionParameters.Key<Boolean>("is_playing_param")

val widgetBgColorIsWhiteKey = booleanPreferencesKey("widget_bg_is_white")
val widgetBgOpacityKey = floatPreferencesKey("widget_bg_opacity")
val widgetMatchDarkKey = booleanPreferencesKey("widget_match_dark")

class MusicPlayerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val songTitle = prefs[widgetSongTitleKey] ?: "Track title"
                val artist = prefs[widgetArtistKey] ?: "Artist"
                val isPlaying = prefs[widgetIsPlayingKey] ?: false
                val artworkPath = prefs[widgetArtworkPathKey]

                val bitmap = loadOptimizedBitmap(artworkPath)

                SamsungStyleWidgetLayout(songTitle, artist, isPlaying, bitmap)
            }
        }
    }

    private fun loadOptimizedBitmap(path: String?): Bitmap? {
        if (path == null) return null
        return try {
            val file = File(path)
            if (file.exists()) {
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(path, options)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    @Composable
    private fun SamsungStyleWidgetLayout(songTitle: String, artist: String, isPlaying: Boolean, bitmap: Bitmap?) {
        val prefs = currentState<Preferences>()

        val isWhite = prefs[widgetBgColorIsWhiteKey] ?: false
        val opacity = prefs[widgetBgOpacityKey] ?: 0.5f
        val matchDark = prefs[widgetMatchDarkKey] ?: true

        val baseColor = if (isWhite && !matchDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
        val finalBgColor = baseColor.copy(alpha = opacity)

        // 1. ROOT BOX: This invisible box fills whatever grid size the user stretches the widget to.
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center // Keeps the widget centered if stretched vertically
        ) {
            // 2. ACTUAL WIDGET CARD: This stays compact and tightly wraps its content.
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(finalBgColor) // Background is now on the compact inner row
                    .cornerRadius(24.dp)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = bitmap?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.music_icon),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    // Slightly larger album art to match the proportions in the Samsung UI
                    modifier = GlanceModifier.size(76.dp).cornerRadius(16.dp)
                )

                Spacer(modifier = GlanceModifier.width(16.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight(), // Takes remaining space
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = songTitle,
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = artist,
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(14.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ControlButton(R.drawable.shuffle, 18.dp, 32.dp, GlanceTheme.colors.onSurface, GlanceModifier.clickable(actionRunCallback<ToggleShuffleAction>()))
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        ControlButton(R.drawable.skip_previous, 22.dp, 36.dp, GlanceTheme.colors.onSurface, GlanceModifier.clickable(actionRunCallback<SkipPreviousAction>()))
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        ControlButton(if (isPlaying) R.drawable.pause else R.drawable.play, 28.dp, 44.dp, GlanceTheme.colors.onSurface, GlanceModifier.clickable(actionRunCallback<TogglePlayAction>(actionParametersOf(isPlayingParamKey to isPlaying))))
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        ControlButton(R.drawable.skip_next, 22.dp, 36.dp, GlanceTheme.colors.onSurface, GlanceModifier.clickable(actionRunCallback<SkipNextAction>()))
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        ControlButton(R.drawable.repeat_off, 18.dp, 32.dp, GlanceTheme.colors.onSurface, GlanceModifier.clickable(actionRunCallback<ToggleRepeatAction>()))
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(iconRes: Int, iconSize: Dp, touchSize: Dp, tint: ColorProvider, actionModifier: GlanceModifier) {
    Box(
        modifier = actionModifier.size(touchSize),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(iconSize)
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

    }
}

class ToggleShuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {

    }
}