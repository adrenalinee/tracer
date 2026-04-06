package malibu.tracer.webflux

import malibu.tracer.io.LimitedByteArrayOutputStream
import malibu.tracer.io.toLimitedString
import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.channels.Channels
import java.util.concurrent.atomic.AtomicBoolean

/**
 * request 당 하나씩 생성됨.
 * 개별 request 실행과정에서 필요한 정보를 맴버 변수로 둘 수 있다(ex: startedTime)
 */
class TracerServerWebExchange(
    private val exchange: ServerWebExchange,
    tracerWebfluxContext: TracerWebfluxContext,
    requestLoggingEnabled: Boolean,
    responseLoggingEnabled: Boolean
): ServerWebExchangeDecorator(exchange) {

    companion object {
        private const val SSE_EVENT_DELIMITER = "\n\n"
        private const val SSE_EVENT_DELIMITER_CRLF = "\r\n\r\n"
    }

    /**
     * request body 읽기가 끝나면 진행할 액션
     */
    lateinit var onRequestBodyReadComplete: () -> Unit

    /**
     * response body write 가 종료되면 진행할 액션
     */
    lateinit var onResponseWriteComplete: () -> Unit

    /**
     * SSE 이벤트가 완성될 때마다 진행할 액션
     */
    lateinit var onResponseSseEvent: (String) -> Unit

    /**
     * request 처리 시작시간
     */
    val startedTime = System.currentTimeMillis()

    /**
     * request body 읽었는지 여부
     */
    var isReadRequestBody: Boolean = false

    /**
     * 복사된 request body
     */
    private val maxPayloadLength = tracerWebfluxContext.maxPayloadLength

    private val requestBodyBaos = LimitedByteArrayOutputStream(maxPayloadLength)

    /**
     * 복사된 response body
     */
    private val responseBodyBaos = LimitedByteArrayOutputStream(maxPayloadLength)

    private val responseWriteCompleted = AtomicBoolean(false)

    private val sseEventBuffer = StringBuilder()


    private val traceRequestBody = tracerWebfluxContext.traceRequestBody && requestLoggingEnabled

    private val decoratedRequest: ServerHttpRequest = if (traceRequestBody &&
        exchange.request.headers.contentLength > 0) {
        object: ServerHttpRequestDecorator(exchange.request) {
            override fun getBody(): Flux<DataBuffer> {
//                println("getBody()")
                return exchange.request.body
                    .doOnNext { partialBody ->
                        //request body 복사
                        Channels.newChannel(requestBodyBaos).write(partialBody.asByteBuffer().asReadOnlyBuffer())
                    }
                    .doOnComplete {
                        isReadRequestBody = true
                        onRequestBodyReadComplete()
                    }

//                    return requestBodyFn(this, delegate.body)
            }
        }
    } else {
        exchange.request
    }

    private val traceResponseBody = tracerWebfluxContext.traceResponseBody && responseLoggingEnabled

    private val decoratedResponse: ServerHttpResponse = object : ServerHttpResponseDecorator(exchange.response) {
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
    }


//    /**
//     * request body 복사
//     */
//    private val requestBodyFn: (ServerHttpRequest, Flux<DataBuffer>) -> Flux<DataBuffer> = { request, body ->
//        body
//            .doOnNext { partialBody ->
//                Channels.newChannel(requestBodyBaos).write(partialBody.asByteBuffer().asReadOnlyBuffer())
//            }
//            .doOnComplete {
//                isReadRequestBody = true
//                onRequestBodyReadComplete()
//            }
//    }

    /**
     * response body 복사
     */
    private val responseBodyFn: (Publisher<out DataBuffer>) -> Publisher<out DataBuffer> = { body ->
        Flux.from(body)
            .doOnNext { partialBody ->
                if (traceResponseBody) {
                    copyResponseBody(partialBody)
                }
            }
    }

    private fun copyResponseBody(partialBody: DataBuffer) {
        val readOnlyBuffer = partialBody.asByteBuffer().asReadOnlyBuffer()
        val stringBuffer = readOnlyBuffer.asReadOnlyBuffer()
        Channels.newChannel(responseBodyBaos).write(readOnlyBuffer)

        if (isSseResponse()) {
            sseEventBuffer.append(readOnlyBufferToString(stringBuffer))
            emitCompletedSseEvents()
        }
    }

    private fun readOnlyBufferToString(readOnlyBuffer: java.nio.ByteBuffer): String {
        if (!readOnlyBuffer.hasRemaining()) {
            return ""
        }

        val bytes = ByteArray(readOnlyBuffer.remaining())
        readOnlyBuffer.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun emitCompletedSseEvents() {
        while (true) {
            val delimiterInfo = findSseDelimiter() ?: return
            val event = sseEventBuffer.substring(0, delimiterInfo.first)
                .trimEnd('\r', '\n')

            sseEventBuffer.delete(0, delimiterInfo.second)

            if (event.isNotBlank()) {
                onResponseSseEvent(event)
            }
        }
    }

    private fun findSseDelimiter(): Pair<Int, Int>? {
        val crlfIndex = sseEventBuffer.indexOf(SSE_EVENT_DELIMITER_CRLF)
        val lfIndex = sseEventBuffer.indexOf(SSE_EVENT_DELIMITER)

        return when {
            crlfIndex >= 0 && (lfIndex < 0 || crlfIndex <= lfIndex) -> crlfIndex to crlfIndex + SSE_EVENT_DELIMITER_CRLF.length
            lfIndex >= 0 -> lfIndex to lfIndex + SSE_EVENT_DELIMITER.length
            else -> null
        }
    }

    private fun isSseResponse(): Boolean {
        return delegateResponseContentType()?.isCompatibleWith(MediaType.TEXT_EVENT_STREAM) == true
    }

    private fun delegateResponseContentType(): MediaType? {
        return exchange.response.headers.contentType
    }

    private fun notifyResponseWriteComplete() {
        if (responseWriteCompleted.compareAndSet(false, true)) {
            if (isSseResponse()) {
                val pendingEvent = sseEventBuffer.toString().trimEnd('\r', '\n')
                if (pendingEvent.isNotBlank()) {
                    onResponseSseEvent(pendingEvent)
                }
                sseEventBuffer.setLength(0)
            }
            onResponseWriteComplete()
        }
    }

    /**
     *
     */
    override fun getRequest(): ServerHttpRequest {
        return decoratedRequest
    }

    /**
     *
     */
    override fun getResponse(): ServerHttpResponse {
        return decoratedResponse
    }

    /**
     * request body byte array 를 String 으로 변환
     * 주의: 호출할때마다 body byte array 를 String 으로 변환하는 작업이 발생합니다.
     */
    fun genRequestBody(): String? {
        return if (requestBodyBaos.size() <= 0) {
            null
        } else {
            requestBodyBaos.toByteArray()
                .toLimitedString(maxPayloadLength, truncated = requestBodyBaos.isTruncated())
        }
    }

    /**
     * response body byte array 를 String 으로 변환
     * 주의: 호출할때마다 body byte array 를 String 으로 변환하는 작업이 발생합니다.
     */
    fun genResponseBody(): String? {
        return if (responseBodyBaos.size() <= 0) {
            null
        } else {
            responseBodyBaos.toByteArray()
                .toLimitedString(maxPayloadLength, truncated = responseBodyBaos.isTruncated())
        }
    }

    /**
     * request body 의 길이
     */
    fun getRequestBodySize(): Int {
        return requestBodyBaos.size()
    }

    /**
     * response body 의 길이
     */
    fun getResponseBodySize(): Int {
        return responseBodyBaos.size()
    }
}
