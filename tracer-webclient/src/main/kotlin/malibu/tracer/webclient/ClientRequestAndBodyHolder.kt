package malibu.tracer.webclient

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerContext
import malibu.tracer.io.LimitedByteArrayOutputStream
import malibu.tracer.io.toLimitedString
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.client.reactive.ClientHttpRequest
import org.springframework.http.client.reactive.ClientHttpRequestDecorator
import org.springframework.web.reactive.function.client.ClientRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.channels.Channels

class ClientRequestAndBodyHolder(
    originRequest: ClientRequest,
    traceSpanId: TraceSpanId?,

    /**
     * body cache 할지 여부
     */
    bodyHold: Boolean,
    private val maxPayloadLength: Int
) {

    private lateinit var tracerWebClientRequest: TracerWebClientRequest

//    lateinit var beforeCommitFn: () -> Unit

    val clientRequest: ClientRequest

    /**
     *
     */
    init {
        val tracerRequestBuilder = ClientRequest.from(originRequest)
        traceSpanId?.spanId?.let { spanId ->
            tracerRequestBuilder.header(TracerContext.DEFAULT_CLIENT_SPAN_ID_HEADER_NAME, spanId.toString())
        }

        if (bodyHold) {
            tracerRequestBuilder.body { outputMessage, context ->
                tracerWebClientRequest = TracerWebClientRequest(outputMessage, maxPayloadLength)
                originRequest.body().insert(tracerWebClientRequest, context)
            }
        }

        clientRequest = tracerRequestBuilder.build()
    }

    /**
     *
     */
    fun genRequestBody(): String? {
        return if (this::tracerWebClientRequest.isInitialized) {
            tracerWebClientRequest.genRequestBody()
        } else {
            null
        }
    }
}

/**
 *
 */
class TracerWebClientRequest(
    delegate: ClientHttpRequest,
    private val maxPayloadLength: Int
): ClientHttpRequestDecorator(delegate) {

    private val requestBodyBaos = LimitedByteArrayOutputStream(maxPayloadLength)

    private val requestBodyFn: (Publisher<out DataBuffer>) -> Publisher<out DataBuffer> = { body ->
        Flux.from(body)
            .doOnNext { partialBody ->
                Channels.newChannel(requestBodyBaos).write(partialBody.asByteBuffer().asReadOnlyBuffer())
            }
    }

    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
        return delegate.writeWith(requestBodyFn(body))
    }

    override fun writeAndFlushWith(body: Publisher<out Publisher<out DataBuffer>>): Mono<Void> {
        return delegate.writeAndFlushWith(
            Flux.from(body).doOnNext { requestBodyFn(it) }
        )
    }

    /**
     *
     */
    fun genRequestBody(): String? {
        return if (requestBodyBaos.size() <= 0) {
            null
        } else {
            requestBodyBaos.toByteArray()
                .toLimitedString(maxPayloadLength, truncated = requestBodyBaos.isTruncated())
        }
    }
}