package com.github.innertube.utils

import io.ktor.util.ContentEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Job
import org.brotli.dec.BrotliInputStream
import kotlin.coroutines.CoroutineContext

@OptIn(InternalAPI::class)
internal object BrotliEncoder : ContentEncoder {
    override val name: String = "br"

    override fun encode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext
    ): ByteReadChannel {
        error("BrotliOutputStream not available (https://github.com/google/brotli/issues/715)")
    }

    override fun encode(
        source: ByteWriteChannel,
        coroutineContext: CoroutineContext
    ): ByteWriteChannel {
        error("BrotliOutputStream not available (https://github.com/google/brotli/issues/715)")
    }

    override fun decode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext
    ): ByteReadChannel = BrotliInputStream(source.toInputStream(coroutineContext[Job])).toByteReadChannel(coroutineContext)
}
