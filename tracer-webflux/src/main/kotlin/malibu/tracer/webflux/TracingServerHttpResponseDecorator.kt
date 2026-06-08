package malibu.tracer.webflux

import malibu.tracer.io.LimitedByteArrayOutputStream
import malibu.tracer.io.toLimitedString
import mu.KotlinLogging
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.channels.Channels
import java.util.concurrent.atomic.AtomicBoolean

class TracingServerHttpResponseDecorator(
    response: ServerHttpResponse,
    private val maxPayloadLength: Int
) : ServerHttpResponseDecorator(response) {

    private val logger = KotlinLogging.logger {}

    var traceResponseBody: Boolean = false

    var onResponseWriteComplete: (() -> Unit)? = null

    private val responseBodyBaos = LimitedByteArrayOutputStream(maxPayloadLength)
    private val responseWriteCompleted = AtomicBoolean(false)

    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
        return delegate.writeWith(responseBodyFn(body))
            .doFinally {
                notifyResponseWriteComplete()
            }
    }

    override fun writeAndFlushWith(body: Publisher<out Publisher<out DataBuffer>>): Mono<Void> {
        return delegate.writeAndFlushWith(
            Flux.from(body)
                .map { innerBody ->
                    Flux.from(innerBody)
                        .doOnNext { partialBody ->
                            if (traceResponseBody) {
                                copyResponseBody(partialBody)
                            }
                        }
                }
        ).doFinally {
            notifyResponseWriteComplete()
        }
    }

    override fun setComplete(): Mono<Void> {
        return delegate.setComplete()
            .doFinally {
                notifyResponseWriteComplete()
            }
    }

    fun genResponseBody(): String? {
        return if (responseBodyBaos.size() <= 0) {
            null
        } else {
            responseBodyBaos.toByteArray()
                .toLimitedString(maxPayloadLength, truncated = responseBodyBaos.isTruncated())
        }
    }

    fun getResponseBodySize(): Int {
        return responseBodyBaos.size()
    }

    private val responseBodyFn: (Publisher<out DataBuffer>) -> Publisher<out DataBuffer> = { body ->
        Flux.from(body)
            .doOnNext { partialBody ->
                if (traceResponseBody) {
                    copyResponseBody(partialBody)
                }
            }
    }

    private fun copyResponseBody(partialBody: DataBuffer) {
        try {
            Channels.newChannel(responseBodyBaos).write(partialBody.asByteBuffer().asReadOnlyBuffer())
        } catch (ex: Exception) {
            logger.debug(ex) { "response body copy failed." }
        }
    }

    private fun notifyResponseWriteComplete() {
        if (responseWriteCompleted.compareAndSet(false, true)) {
            try {
                onResponseWriteComplete?.invoke()
            } catch (ex: Exception) {
                logger.debug(ex) { "response write complete callback failed." }
            }
        }
    }
}
