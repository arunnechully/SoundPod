package com.github.soundpod.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Locale

/**
 * A helper composable that sets up the Voice Search launcher and returns
 * a lambda function to trigger it.
 *
 * @param context The current context (for Toasts)
 * @param onSpeechResult Callback when speech is successfully recognized
 */
@Composable
fun rememberVoiceSearchLauncher(
    context: Context,
    onSpeechResult: (String) -> Unit
): () -> Unit {

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                onSpeechResult(spokenText)
            }
        }
    }

    return remember {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
            }
            try {
                voiceLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Voice search not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
}