package com.github.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ContinuationItemRenderer(
    val trigger: String? = null,
    val continuationEndpoint: Continuation.ContinuationEndpoint? = null,
    val continuationCommand: Continuation.ContinuationCommand? = null
) {
    val continuation: String?
        get() = continuationEndpoint?.continuationCommand?.token
            ?: continuationCommand?.token
}
