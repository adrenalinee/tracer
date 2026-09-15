package malibu.tracer.webflux

import malibu.tracer.io.LimitedByteArrayOutputStream
import malibu.tracer.io.toLimitedString
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Flux
import java.nio.channels.Channels

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

    /**
     * request body 읽기가 끝나면 진행할 액션
     */
    lateinit var onRequestBodyReadComplete: () -> Unit

    /**
     * response body write 가 종료되면 진행할 액션
     */
    lateinit var onResponseWriteComplete: () -> Unit

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

    private val decoratedResponse: TracingServerHttpResponseDecorator = (
        findTracingResponse(exchange.response)
            ?: TracingServerHttpResponseDecorator(exchange.response, maxPayloadLength)
        ).also { response ->
        response.traceResponseBody = tracerWebfluxContext.traceResponseBody && responseLoggingEnabled
        response.onResponseWriteComplete = {
            onResponseWriteComplete()
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
        return decoratedResponse.genResponseBody()
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
        return decoratedResponse.getResponseBodySize()
    }

    private tailrec fun findTracingResponse(response: ServerHttpResponse): TracingServerHttpResponseDecorator? {
        return when (response) {
            is TracingServerHttpResponseDecorator -> response
            is ServerHttpResponseDecorator -> findTracingResponse(response.delegate)
            else -> null
        }
    }
}
