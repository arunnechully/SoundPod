package com.github.musicyou.service

import androidx.media3.common.PlaybackException

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayableFormatNotFoundException :
    PlaybackException("Playable format not found", null, ERROR_CODE_REMOTE_ERROR)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class UnplayableException :
    PlaybackException("Unplayable", null, ERROR_CODE_REMOTE_ERROR)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class LoginRequiredException :
    PlaybackException("Login required", null, ERROR_CODE_REMOTE_ERROR)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class VideoIdMismatchException :
    PlaybackException("Video id mismatch", null, ERROR_CODE_REMOTE_ERROR)