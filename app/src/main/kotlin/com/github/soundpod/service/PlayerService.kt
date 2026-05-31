@file:Suppress("DEPRECATION")

package com.github.soundpod.service

import android.app.Notification
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.github.innertube.models.NavigationEndpoint
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.soundpod.db
import com.github.soundpod.models.Format
import com.github.soundpod.query
import com.github.soundpod.utils.InvincibleService
import com.github.soundpod.utils.broadCastPendingIntent
import com.github.soundpod.utils.intent
import com.github.soundpod.utils.isAtLeastAndroid13
import com.github.soundpod.utils.isAtLeastAndroid8
import com.github.soundpod.utils.isInvincibilityEnabledKey
import com.github.soundpod.utils.isShowingThumbnailInLockscreenKey
import com.github.soundpod.utils.pauseOnAppCloseKey
import com.github.soundpod.utils.persistentQueueKey
import com.github.soundpod.utils.playbackPitchKey
import com.github.soundpod.utils.playbackSpeedKey
import com.github.soundpod.utils.preferences
import com.github.soundpod.utils.queueLoopEnabledKey
import com.github.soundpod.utils.resumePlaybackWhenDeviceConnectedKey
import com.github.soundpod.utils.shouldBePlaying
import com.github.soundpod.utils.skipSilenceKey
import com.github.soundpod.utils.stopAfterCurrentKey
import com.github.soundpod.utils.trackLoopEnabledKey
import com.github.soundpod.utils.volumeNormalizationKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import android.os.Binder as AndroidBinder

@androidx.annotation.OptIn(UnstableApi::class)
class PlayerService : InvincibleService(), Player.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mediaSessionManager: PlayerMediaSessionManager

    private lateinit var cacheManager: PlayerCacheManager
    private lateinit var player: ExoPlayer

    private lateinit var widgetUpdater: WidgetUpdater

    private lateinit var audioEffectManager: AudioEffectManager

    private lateinit var playerNotificationManager: PlayerNotificationManager

    private lateinit var sleepTimerManager: SleepTimerManager

    private lateinit var radioManager: YouTubeRadioManager

    private lateinit var queueManager: QueuePersistenceManager
    private lateinit var bitmapProvider: BitmapProvider
    private lateinit var mediaSourceProvider: PlayerMediaSourceProvider

    private val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()

    private var isPersistentQueueEnabled = false
    private var isShowingThumbnailInLockscreen = true
    override var isInvincibilityEnabled = false

    private var audioManager: AudioManager? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    private val binder = Binder()

    private var isNotificationStarted = false

    override val notificationId: Int
        get() = NOTIFICATION_ID

    private val mediaItemState = MutableStateFlow<MediaItem?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val isLikedState = mediaItemState
        .flatMapMerge { item ->
            item?.mediaId?.let {
                db
                    .likedAt(it)
                    .distinctUntilChanged()
                    .cancellable()
            } ?: flowOf(null)
        }
        .map { it != null }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    override fun onBind(intent: Intent?): AndroidBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        widgetUpdater = WidgetUpdater(applicationContext, coroutineScope)

        bitmapProvider = BitmapProvider(
            context = applicationContext,
            bitmapSize = (256 * resources.displayMetrics.density).roundToInt(),
            colorProvider = { isSystemInDarkMode ->
                if (isSystemInDarkMode) Color.BLACK else Color.WHITE
            }
        )

        preferences.registerOnSharedPreferenceChangeListener(this)

        val preferences = preferences
        isPersistentQueueEnabled = preferences.getBoolean(persistentQueueKey, false)
        isInvincibilityEnabled = preferences.getBoolean(isInvincibilityEnabledKey, false)
        isShowingThumbnailInLockscreen = preferences.getBoolean(isShowingThumbnailInLockscreenKey, false)

        cacheManager = PlayerCacheManager(this)

        mediaSourceProvider = PlayerMediaSourceProvider(this, cacheManager)

        player = ExoPlayer.Builder(this, createRendersFactory(), mediaSourceProvider.createMediaSourceFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setUsePlatformDiagnostics(false)
            .build()

        player.repeatMode = when {
            preferences.getBoolean(trackLoopEnabledKey, false) -> Player.REPEAT_MODE_ONE
            preferences.getBoolean(queueLoopEnabledKey, false) -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }

        player.skipSilenceEnabled = preferences.getBoolean(skipSilenceKey, false)
        player.pauseAtEndOfMediaItems = preferences.getBoolean(stopAfterCurrentKey, false)
        player.playbackParameters = androidx.media3.common.PlaybackParameters(
            preferences.getFloat(playbackSpeedKey, 1f),
            preferences.getFloat(playbackPitchKey, 1f)
        )
        player.addListener(this)
        player.addAnalyticsListener(PlaybackStatsListener(false, PlaybackAnalyticsTracker()))

        audioEffectManager = AudioEffectManager(this, player, coroutineScope)
        radioManager = YouTubeRadioManager(player, coroutineScope)

        mediaSessionManager = PlayerMediaSessionManager(
            context = this,
            player = player,
            coroutineScope = coroutineScope,
            onPlayAction = { play() },
            onLikeAction = { likeAction() },
            onLoopAction = { loopAction() }
        )

        playerNotificationManager = PlayerNotificationManager(
            context = this,
            player = player,
            mediaSessionToken = MediaSessionCompat.Token.fromToken(mediaSessionManager.mediaSession.sessionToken),
            bitmapProvider = bitmapProvider,
            onPlayAction = { play() },
            onCoverArtReady = { maybeShowSongCoverInLockScreen() }
        )

        sleepTimerManager = SleepTimerManager(this, coroutineScope, playerNotificationManager.notificationManager) {
            stopSelf()
            exitProcess(0)
        }

        queueManager = QueuePersistenceManager(player, coroutineScope) {
            isNotificationStarted = true
            startForegroundService(this@PlayerService, intent<PlayerService>())
            val notif = notification()
            if (notif != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notif)
                }
            }
        }

        queueManager.restoreQueue(isPersistentQueueEnabled)

        coroutineScope.launch {
            isLikedState
                .onEach { withContext(Dispatchers.Main) { updatePlaybackState() } }
                .collect()
        }

        maybeResumePlaybackWhenDeviceConnected()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (preferences.getBoolean(pauseOnAppCloseKey, false)) {
            stopSelf()
        } else if (!player.shouldBePlaying) {
            broadCastPendingIntent<NotificationDismissReceiver>().send()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        queueManager.saveQueue(isPersistentQueueEnabled)
        preferences.unregisterOnSharedPreferenceChangeListener(this)

        player.removeListener(this)
        player.stop()
        player.release()

        playerNotificationManager.notificationManager?.cancel(NOTIFICATION_ID)
        playerNotificationManager.release()

        mediaSessionManager.release()
        cacheManager.release()

        audioEffectManager.release()

        super.onDestroy()
    }

    override fun shouldBeInvincible(): Boolean {
        return !player.shouldBePlaying
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (bitmapProvider.setDefaultBitmap() && player.currentMediaItem != null) {
            playerNotificationManager.notificationManager?.notify(NOTIFICATION_ID, notification())
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaItemState.update { mediaItem }

        maybeRecoverPlaybackError()
        audioEffectManager.maybeNormalizeVolume()
        maybeProcessRadio()
        maybeFetchLyrics(mediaItem)
        prefetchNextTrack()

        if (mediaItem == null) {
            bitmapProvider.listener?.invoke(null)
        } else if (mediaItem.mediaMetadata.artworkUri == bitmapProvider.lastUri) {
            bitmapProvider.listener?.invoke(bitmapProvider.lastBitmap)
        }

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            mediaSessionManager.updateQueue(player.currentTimeline)
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
            mediaSessionManager.updateQueue(timeline)
            prefetchNextTrack()
        }
    }

    private fun prefetchNextTrack() {
        val currentIndex = player.currentMediaItemIndex
        val totalItems = player.mediaItemCount

        // Prefetch the next 3 tracks
        for (i in 1..3) {
            val nextIndex = currentIndex + i
            if (nextIndex >= totalItems || nextIndex < 0) break

            val nextMediaItem = player.getMediaItemAt(nextIndex)
            val videoId = nextMediaItem.mediaId

            coroutineScope.launch {
                runCatching {
                    // 1. Resolve URL (NewPipe)
                    val uri = mediaSourceProvider.resolveUrl(videoId)

                    // 2. Cache track (1MB)
                    cacheTrack(videoId, uri)

                    // 3. Prefetch lyrics
                    LyricsFetcher.fetchLyrics(videoId)

                    // 4. Prefetch extra metadata from InnerTube
                    Innertube.player(videoId)?.onSuccess { response ->
                        val highestQualityFormat = response.streamingData?.highestQualityFormat
                        val loudnessDb = response.playerConfig?.audioConfig?.normalizedLoudnessDb
                        query {
                            db.insert(
                                Format(
                                    songId = videoId,
                                    itag = highestQualityFormat?.itag,
                                    mimeType = highestQualityFormat?.mimeType,
                                    bitrate = highestQualityFormat?.bitrate,
                                    contentLength = highestQualityFormat?.contentLength,
                                    lastModified = highestQualityFormat?.lastModified,
                                    loudnessDb = loudnessDb
                                )
                            )
                        }
                    }

                    // 5. Prefetch artwork
                    nextMediaItem.mediaMetadata.artworkUri?.let { artworkUri ->
                        bitmapProvider.load(artworkUri) { }
                    }
                }
            }
        }
    }

    private fun cacheTrack(videoId: String, uri: Uri) {
        if (cacheManager.cache.isCached(videoId, 0, 100 * 1024L)) return

        val dataSpec = DataSpec.Builder()
            .setUri(uri)
            .setKey(videoId)
            .setPosition(0)
            .setLength(1024 * 1024L) // Cache the first 1MB
            .build()

        val upstreamDataSource = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .createDataSource()

        val cacheDataSource = CacheDataSource.Factory()
            .setCache(cacheManager.cache)
            .setUpstreamDataSourceFactory { upstreamDataSource }
            .createDataSource()

        runCatching {
            CacheWriter(cacheDataSource, dataSpec, null, null).cache()
        }
    }

    private fun maybeRecoverPlaybackError() {
        if (player.playerError != null) player.prepare()
    }
    private fun maybeProcessRadio() {
        radioManager.processNextBatch()
    }

    private fun maybeFetchLyrics(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId ?: return
        mediaItem.mediaMetadata

        coroutineScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                if (player.duration == C.TIME_UNSET) 0L else player.duration
            }

            LyricsFetcher.fetchLyrics(mediaId)
        }
    }

    private fun maybeShowSongCoverInLockScreen() {
        mediaSessionManager.updateMetadata(
            bitmapProvider = bitmapProvider,
            isAtLeastAndroid13 = isAtLeastAndroid13,
            isShowingThumbnailInLockscreen = isShowingThumbnailInLockscreen
        )
    }

    private fun maybeResumePlaybackWhenDeviceConnected() {

        if (preferences.getBoolean(resumePlaybackWhenDeviceConnectedKey, false)) {
            if (audioManager == null) {
                audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?
            }

            audioDeviceCallback = object : AudioDeviceCallback() {
                private fun canPlayMusic(audioDeviceInfo: AudioDeviceInfo): Boolean {
                    if (!audioDeviceInfo.isSink) return false

                    return audioDeviceInfo.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            audioDeviceInfo.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            (isAtLeastAndroid8 && audioDeviceInfo.type == AudioDeviceInfo.TYPE_USB_HEADSET)
                }

                override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
                    if (!player.isPlaying && addedDevices.any(::canPlayMusic)) {
                        player.play()
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = Unit
            }

            audioManager?.registerAudioDeviceCallback(audioDeviceCallback, handler)

        } else {
            audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback)
            audioDeviceCallback = null
        }
    }

    private fun updatePlaybackState() = coroutineScope.launch {
        mediaSessionManager.updatePlaybackState(isLikedState.value)
    }


    override fun onEvents(player: Player, events: Player.Events) {

        if (events.containsAny(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED
            )) {
            mediaSessionManager.updateMetadata(bitmapProvider, isAtLeastAndroid13, isShowingThumbnailInLockscreen)
        }

        //Playback state updates
        if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_POSITION_DISCONTINUITY)) {
            updatePlaybackState()
        }

        //Widget updates
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION
            )
        ) {
            widgetUpdater.updateWidget(player, bitmapProvider.bitmap)
        }

        //Notification Lifecycle
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY
            )
        ) {
            val notification = notification()

            if (notification == null) {
                isNotificationStarted = false
                makeInvincible(false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }

                audioEffectManager.sendCloseEqualizerIntent()
                playerNotificationManager.notificationManager?.cancel(NOTIFICATION_ID)
                return
            }

            if (player.shouldBePlaying && !isNotificationStarted) {
                isNotificationStarted = true
                startForegroundService(this@PlayerService, intent<PlayerService>())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                makeInvincible(false)
                audioEffectManager.sendOpenEqualizerIntent()
            } else {
                if (!player.shouldBePlaying) {
                    isNotificationStarted = false

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(false)
                    }

                    makeInvincible(true)
                    audioEffectManager.sendCloseEqualizerIntent()
                }
                playerNotificationManager.notificationManager?.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            persistentQueueKey -> if (sharedPreferences != null) {
                isPersistentQueueEnabled =
                    sharedPreferences.getBoolean(key, isPersistentQueueEnabled)
            }

            volumeNormalizationKey -> audioEffectManager.maybeNormalizeVolume()

            playbackSpeedKey, playbackPitchKey -> if (sharedPreferences != null) {
                player.playbackParameters = androidx.media3.common.PlaybackParameters(
                    sharedPreferences.getFloat(playbackSpeedKey, 1f),
                    sharedPreferences.getFloat(playbackPitchKey, 1f)
                )
            }

            resumePlaybackWhenDeviceConnectedKey -> maybeResumePlaybackWhenDeviceConnected()

            isInvincibilityEnabledKey -> if (sharedPreferences != null) {
                isInvincibilityEnabled =
                    sharedPreferences.getBoolean(key, isInvincibilityEnabled)
            }

            skipSilenceKey -> if (sharedPreferences != null) {
                player.skipSilenceEnabled = sharedPreferences.getBoolean(key, false)
            }

            stopAfterCurrentKey -> if (sharedPreferences != null) {
                player.pauseAtEndOfMediaItems = sharedPreferences.getBoolean(key, false)
            }

            isShowingThumbnailInLockscreenKey -> {
                if (sharedPreferences != null) {
                    isShowingThumbnailInLockscreen = sharedPreferences.getBoolean(key, true)
                }
                maybeShowSongCoverInLockScreen()
            }

            trackLoopEnabledKey, queueLoopEnabledKey -> {
                player.repeatMode = when {
                    preferences.getBoolean(trackLoopEnabledKey, false) -> Player.REPEAT_MODE_ONE
                    preferences.getBoolean(queueLoopEnabledKey, false) -> Player.REPEAT_MODE_ALL
                    else -> Player.REPEAT_MODE_OFF
                }
                updatePlaybackState()
            }
        }
    }

    override fun notification(): Notification? {
        return playerNotificationManager.getNotification()
    }
    private fun createRendersFactory(): RenderersFactory {
        val audioSink = DefaultAudioSink.Builder(applicationContext)
            .setEnableFloatOutput(false)
            .setAudioProcessorChain(
                DefaultAudioProcessorChain(
                    emptyArray(),
                    SilenceSkippingAudioProcessor(2_000_000, 0.01f, 2_000_000, 0, 256),
                    SonicAudioProcessor()
                )
            )
            .build()

        return RenderersFactory { handler: Handler?, _, audioListener: AudioRendererEventListener?, _, _ ->
            arrayOf(
                MediaCodecAudioRenderer(
                    this,
                    MediaCodecSelector.DEFAULT,
                    handler,
                    audioListener,
                    audioSink
                )
            )
        }
    }

    inner class Binder : AndroidBinder() {
        val player get() = this@PlayerService.player
        val cache get() = this@PlayerService.cacheManager.cache
        val mediaSession get() = this@PlayerService.mediaSessionManager.mediaSession

        val sleepTimerMillisLeft get() = this@PlayerService.sleepTimerManager.millisLeft

        fun startSleepTimer(delay: Long) = sleepTimerManager.startTimer(delay)
        fun cancelSleepTimer() = sleepTimerManager.cancelTimer()

        fun setupRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) = radioManager.setupRadio(endpoint)
        fun playRadio(endpoint: NavigationEndpoint.Endpoint.Watch?) = radioManager.playRadio(endpoint)
        fun stopRadio() = radioManager.stop()
    }

    private fun likeAction() = mediaItemState.value?.let { mediaItem ->
        query {
            runCatching {
                db.like(
                    songId = mediaItem.mediaId,
                    likedAt = if (isLikedState.value) null else System.currentTimeMillis()
                )
            }
        }
    }

    private fun loopAction() {
        preferences.edit {
            when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> {
                    putBoolean(queueLoopEnabledKey, true)
                }

                Player.REPEAT_MODE_ALL -> {
                    putBoolean(queueLoopEnabledKey, false)
                    putBoolean(trackLoopEnabledKey, true)
                }

                Player.REPEAT_MODE_ONE -> {
                    putBoolean(trackLoopEnabledKey, false)
                }
            }
        }
    }


    private fun play() {
        if (player.playerError != null) player.prepare()
        else if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition(0)
        else player.play()
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
    }
}
