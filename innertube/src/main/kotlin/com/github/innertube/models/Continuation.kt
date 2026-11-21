package com.github.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Continuation(
    @JsonNames("nextContinuationData", "nextRadioContinuationData", "reloadContinuationData")
    val nextContinuationData: Data? = null,
    val continuationEndpoint: ContinuationEndpoint? = null,
    val continuationCommand: ContinuationCommand? = null
) {
    @Serializable
    data class Data(
        val continuation: String? = null
    )

    @Serializable
    data class ContinuationEndpoint(
        val continuationCommand: ContinuationCommand? = null
    )

    @Serializable
    data class ContinuationCommand(
        val token: String? = null
    )

    val continuation: String?
        get() = nextContinuationData?.continuation
            ?: continuationEndpoint?.continuationCommand?.token
            ?: continuationCommand?.token
}
