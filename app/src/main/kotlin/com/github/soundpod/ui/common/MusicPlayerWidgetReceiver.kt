package com.github.soundpod.ui.common

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MusicPlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    // Link the receiver to your UI class
    override val glanceAppWidget: GlanceAppWidget = MusicPlayerWidget()
}