package com.github.core.ui.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun Context.streamVolumeFlow(
    stream: Int = AudioManager.STREAM_MUSIC,
    @ContextCompat.RegisterReceiverFlags
    flags: Int = ContextCompat.RECEIVER_NOT_EXPORTED
) = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras?.volumeChangedIntentBundle ?: return
            if (stream == extras.streamType) trySend(extras.value)
        }
    }

    ContextCompat.registerReceiver(
        /* context = */ this@Context,
        /* receiver = */ receiver,
        /* filter = */ IntentFilter(VolumeChangedIntentBundleAccessor.ACTION),
        /* flags = */ flags
    )
    awaitClose { unregisterReceiver(receiver) }
}

class VolumeChangedIntentBundleAccessor(val bundle: Bundle = Bundle()) : BundleAccessor {
    companion object {
        const val ACTION = "android.media.VOLUME_CHANGED_ACTION"

        fun bundle(block: VolumeChangedIntentBundleAccessor.() -> Unit) =
            VolumeChangedIntentBundleAccessor().apply(block).bundle
    }

    var streamType by bundle.int("android.media.EXTRA_VOLUME_STREAM_TYPE")
    var value by bundle.int("android.media.EXTRA_VOLUME_STREAM_VALUE")
}

inline val Bundle.volumeChangedIntentBundle get() = VolumeChangedIntentBundleAccessor(this)
