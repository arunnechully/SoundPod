package com.github.soundpod.ui.screens.player

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.github.core.ui.LocalAppearance
import com.github.soundpod.LocalPlayerServiceBinder
import com.github.soundpod.R
import com.github.soundpod.db
import com.github.soundpod.models.Info
import com.github.soundpod.ui.components.SettingsCard
import com.github.soundpod.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun TrackDetails() {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var mediaItem by remember(player) { mutableStateOf(player?.currentMediaItem) }
    var tracks by remember(player) { mutableStateOf(player?.currentTracks) }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}

        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_PLAYLIST_METADATA_CHANGED
                    )
                ) {
                    mediaItem = player.currentMediaItem
                }

                if (events.contains(Player.EVENT_TRACKS_CHANGED)) {
                    tracks = player.currentTracks
                }
            }
        }

        player.addListener(listener)
        mediaItem = player.currentMediaItem
        tracks = player.currentTracks

        onDispose {
            player.removeListener(listener)
        }
    }

    val song by remember(mediaItem?.mediaId) {
        mediaItem?.mediaId?.let { db.song(it) } ?: flowOf(null)
    }.collectAsState(initial = null)

    val formatFromDb by remember(mediaItem?.mediaId) {
        mediaItem?.mediaId?.let { db.format(it) } ?: flowOf(null)
    }.collectAsState(initial = null)

    val albumInfo by produceState<Info?>(initialValue = null, key1 = mediaItem?.mediaId) {
        value = withContext(Dispatchers.IO) {
            mediaItem?.mediaId?.let { db.songAlbumInfo(it) }
        }
    }

    val audioFormat = remember(mediaItem, tracks) {
        tracks?.groups?.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?.getTrackFormat(0)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(colorPalette.text.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            val artworkUri = mediaItem?.mediaMetadata?.artworkUri
            if (artworkUri != null) {
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.music_icon),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = colorPalette.text.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = mediaItem?.mediaMetadata?.title?.toString()
                ?: stringResource(id = R.string.unknown),
            style = typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colorPalette.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Artist
        Text(
            text = mediaItem?.mediaMetadata?.artist?.toString()
                ?: stringResource(id = R.string.unknown),
            style = typography.bodyLarge,
            color = colorPalette.text.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingsCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                DetailItem(
                    label = stringResource(id = R.string.album),
                    value = mediaItem?.mediaMetadata?.albumTitle?.toString()
                        ?: albumInfo?.name ?: stringResource(id = R.string.unknown)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = colorPalette.text.copy(alpha = 0.1f)
                )

                DetailItem(
                    label = stringResource(id = R.string.track_length),
                    value = song?.durationText
                        ?: mediaItem?.mediaMetadata?.extras?.getString("durationText")
                        ?: stringResource(id = R.string.unknown)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = colorPalette.text.copy(alpha = 0.1f)
                )

                DetailItem(
                    label = stringResource(id = R.string.format),
                    value = formatFromDb?.mimeType ?: audioFormat?.sampleMimeType ?: stringResource(
                        id = R.string.unknown
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = colorPalette.text.copy(alpha = 0.1f)
                )

                val bitrate = formatFromDb?.bitrate ?: audioFormat?.bitrate?.toLong()
                if (bitrate != null && bitrate > 0) {
                    DetailItem(
                        label = stringResource(id = R.string.bitrate),
                        value = stringResource(id = R.string.kbps, bitrate / 1000)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colorPalette.text.copy(alpha = 0.1f)
                    )
                }

                DetailItem(
                    label = stringResource(id = R.string.path_id),
                    value = mediaItem?.mediaId ?: stringResource(id = R.string.unknown),
                    trailingContent = {
                        mediaItem?.mediaId?.let { id ->
                            val label = stringResource(id = R.string.id)
                            val copiedMessage = stringResource(id = R.string.copy_to_clipboard)
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        clipboardManager.setClipEntry(
                                            ClipEntry(
                                                ClipData.newPlainText(
                                                    label,
                                                    id
                                                )
                                            )
                                        )
                                    }
                                    context.toast(copiedMessage)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(id = R.string.copy),
                                    modifier = Modifier
                                        .size(18.dp),
                                    tint = colorPalette.text
                                )
                            }
                        }
                    }
                )

                val loudness = formatFromDb?.loudnessDb
                if (loudness != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = colorPalette.text.copy(alpha = 0.1f)
                    )

                    DetailItem(
                        label = stringResource(id = R.string.loudness),
                        value = stringResource(id = R.string.decibel, loudness.toString())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val (colorPalette) = LocalAppearance.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = typography.labelMedium,
                color = colorPalette.accent,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = typography.bodyLarge,
                color = colorPalette.text,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}