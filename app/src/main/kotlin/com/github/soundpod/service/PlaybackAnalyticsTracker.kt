package com.github.soundpod.service

import android.database.SQLException
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import com.github.soundpod.db
import com.github.soundpod.models.Event
import com.github.soundpod.query

@UnstableApi
class PlaybackAnalyticsTracker : PlaybackStatsListener.Callback {

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats
    ) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
        val totalPlayTimeMs = playbackStats.totalPlayTimeMs

        if (totalPlayTimeMs > 5000) {
            query {
                db.incrementTotalPlayTimeMs(mediaItem.mediaId, totalPlayTimeMs)
            }
        }

        if (totalPlayTimeMs > 30000) {
            query {
                try {
                    db.insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = System.currentTimeMillis(),
                            playTime = totalPlayTimeMs
                        )
                    )
                } catch (_: SQLException) {
                }
            }
        }
    }
}