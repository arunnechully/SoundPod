package com.github.soundpod.service

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.media3.common.Player
import com.github.soundpod.ui.common.MusicPlayerWidget
import com.github.soundpod.ui.common.widgetArtistKey
import com.github.soundpod.ui.common.widgetArtworkPathKey
import com.github.soundpod.ui.common.widgetIsPlayingKey
import com.github.soundpod.ui.common.widgetSongTitleKey
import com.github.soundpod.utils.shouldBePlaying
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class WidgetUpdater(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    fun updateWidget(player: Player, currentBitmap: Bitmap?) {
        val currentTitle = player.mediaMetadata.title?.toString() ?: "Not Playing"
        val currentArtist = player.mediaMetadata.artist?.toString() ?: "SoundPod"
        val isCurrentlyPlaying = player.shouldBePlaying

        coroutineScope.launch(Dispatchers.IO) {
            var finalArtworkPath: String? = null

            val cacheFiles = context.cacheDir.listFiles { _, name ->
                name.startsWith("widget_thumb_")
            }
            cacheFiles?.forEach { it.delete() }

            if (currentBitmap != null) {
                val uniqueFileName = "widget_thumb_${System.currentTimeMillis()}.jpg"
                val thumbFile = File(context.cacheDir, uniqueFileName)

                try {
                    FileOutputStream(thumbFile).use { out ->
                        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                    }
                    finalArtworkPath = thumbFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(MusicPlayerWidget::class.java).forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[widgetSongTitleKey] = currentTitle
                    prefs[widgetArtistKey] = currentArtist
                    prefs[widgetIsPlayingKey] = isCurrentlyPlaying

                    if (finalArtworkPath != null) {
                        prefs[widgetArtworkPathKey] = finalArtworkPath
                    } else {
                        prefs.remove(widgetArtworkPathKey)
                    }
                }
                MusicPlayerWidget().update(context, glanceId)
            }
        }
    }
}