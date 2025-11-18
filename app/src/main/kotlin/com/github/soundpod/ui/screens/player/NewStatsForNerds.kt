package com.github.soundpod.ui.screens.player

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.Database
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.models.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun NewStatsForNerds(
    mediaId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current ?: return

    var cachedBytes by remember(mediaId) {
        mutableLongStateOf(binder.cache.getCachedBytes(mediaId, 0, -1))
    }

    var format by remember {
        mutableStateOf<Format?>(null)
    }

    LaunchedEffect(mediaId) {
        Database.format(mediaId).distinctUntilChanged().collectLatest { currentFormat ->
            if (currentFormat?.itag == null) {
                binder.player.currentMediaItem?.takeIf { it.mediaId == mediaId }?.let { mediaItem ->
                    withContext(Dispatchers.IO) {
                        delay(2000)
                        Innertube.player(videoId = mediaId)?.onSuccess { response ->
                            response.streamingData?.highestQualityFormat?.let { format ->
                                Database.insert(mediaItem)
                                Database.insert(
                                    Format(
                                        songId = mediaId,
                                        itag = format.itag,
                                        mimeType = format.mimeType,
                                        bitrate = format.bitrate,
                                        loudnessDb = response.playerConfig?.audioConfig?.normalizedLoudnessDb,
                                        contentLength = format.contentLength,
                                        lastModified = format.lastModified
                                    )
                                )
                            }
                        }
                    }
                }
            } else format = currentFormat
        }
    }

    DisposableEffect(mediaId) {
        val listener = object : Cache.Listener {
            override fun onSpanAdded(cache: Cache, span: CacheSpan) {
                cachedBytes += span.length
            }

            override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
                cachedBytes -= span.length
            }

            override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) = Unit
        }

        binder.cache.addListener(mediaId, listener)

        onDispose {
            binder.cache.removeListener(mediaId, listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() }, // tap background to close
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Card-like content box
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = stringResource(id = R.string.information),
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(id = R.string.id), fontWeight = FontWeight.Bold)
                    Text(stringResource(id = R.string.itag), fontWeight = FontWeight.Bold)
                    Text(stringResource(id = R.string.bitrate), fontWeight = FontWeight.Bold)
                    Text(stringResource(id = R.string.size), fontWeight = FontWeight.Bold)
                    Text(stringResource(id = R.string.cached), fontWeight = FontWeight.Bold)
                    Text(stringResource(id = R.string.loudness), fontWeight = FontWeight.Bold)
                }

                Column {
                    Text(mediaId)
                    Text(format?.itag?.toString() ?: "Unknown")
                    Text(format?.bitrate?.let { "${it / 1000} kbps" } ?: "Unknown")
                    Text(
                        format?.contentLength?.let { Formatter.formatShortFileSize(context, it) }
                            ?: "Unknown"
                    )
                    Text(
                        Formatter.formatShortFileSize(context, cachedBytes) +
                                format?.contentLength?.let {
                                    " (${(cachedBytes.toFloat() / it * 100).roundToInt()}%)"
                                }.orEmpty()
                    )
                    Text(format?.loudnessDb?.let { "%.2f dB".format(it) } ?: "Unknown")
                }
            }

            Button(onClick = { binder.cache.removeResource(mediaId) }) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Clear Cache")
            }

            Spacer(modifier = Modifier.size(12.dp))

            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    }

}