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

// Helper object to hold the MediaController instance, making it accessible to Compose.
// The 'by mutableStateOf' makes it observable, triggering UI recomposition on change.
object MediaControllerHolder {
    var controller: MediaController? by mutableStateOf(null)
}

class MainActivity : ComponentActivity() {

    // Future object to manage the asynchronous connection to the MediaSessionService
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Create a ComponentName pointing to your MediaSessionService
        val serviceComponentName = ComponentName(this, PlaybackService::class.java)

        // 2. Create the SessionToken using Context and ComponentName (Fixes the constructor error)
        val sessionToken = SessionToken(this, serviceComponentName)

        // 3. Start the asynchronous connection process to the service
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        // 4. Set a listener to update the global controller state when the connection is established
        controllerFuture.addListener({
            // Get the successfully connected controller and store it in the observable holder
            MediaControllerHolder.controller = controllerFuture.get()
        }, MoreExecutors.directExecutor())

        setContent {
            MainNavigation()

            // 5. Cleanup: Use DisposableEffect to release resources when the activity is destroyed
            DisposableEffect(Unit) {
                onDispose {
                    // Release the MediaController connection
                    MediaController.releaseFuture(controllerFuture)
                    MediaControllerHolder.controller = null // Clear the observable reference
                }
            }
        }
    }
}