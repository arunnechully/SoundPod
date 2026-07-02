package com.github.soundpod.enums

enum class DownloadDiskCacheMaxSize {
    `512MB`,
    `1GB`,
    `2GB`,
    `5GB`,
    `10GB`,
    `20GB`,
    Unlimited;

    val bytes: Long
        get() = when (this) {
            `512MB` -> 512
            `1GB` -> 1024
            `2GB` -> 2048
            `5GB` -> 5120
            `10GB` -> 10240
            `20GB` -> 20480
            Unlimited -> 0
        } * 1000 * 1000L
}
