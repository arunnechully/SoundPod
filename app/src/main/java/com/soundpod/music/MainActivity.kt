package com.soundpod.music

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.soundpod.music.player.PlaybackService
import com.soundpod.music.ui.screens.MainNavigation
object MediaControllerHolder {
    var controller: MediaController? by mutableStateOf(null)
}

class MainActivity : ComponentActivity() {
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serviceComponentName = ComponentName(this, PlaybackService::class.java)

        val sessionToken = SessionToken(this, serviceComponentName)

        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            MediaControllerHolder.controller = controllerFuture.get()
        }, MoreExecutors.directExecutor())

        setContent {
            MainNavigation()

            DisposableEffect(Unit) {
                onDispose {

                    MediaController.releaseFuture(controllerFuture)
                    MediaControllerHolder.controller = null
                }
            }
        }
    }
}