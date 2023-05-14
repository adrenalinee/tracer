package malibu.tracer.webflux

import org.reactivestreams.Publisher
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels

/**
 * request 당 하나씩 생성됨.
 * 개별 request 실행과정에서 필요한 정보를 맴버 변수로 둘 수 있다(ex: startedTime)
 */
class TracerServerWebExchange(
    private val exchange: ServerWebExchange,
    tracerWebfluxContext: TracerWebfluxContext
): ServerWebExchangeDecorator(exchange) {

    /**
     * request body 읽기가 끝나면 진행할 액션
     */
    lateinit var onRequestBodyReadComplete: () -> Unit

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
    private val requestBodyBaos = ByteArrayOutputStream()

    /**
     * 복사된 response body
     */
    private val responseBodyBaos = ByteArrayOutputStream()


    private val decoratedRequest: ServerHttpRequest = if (tracerWebfluxContext.traceRequestBody &&
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

    private val decoratedResponse: ServerHttpResponse = if (tracerWebfluxContext.traceResponseBody) {
        object : ServerHttpResponseDecorator(exchange.response) {
            override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                return delegate.writeWith(responseBodyFn(body))
            }

            override fun writeAndFlushWith(body: Publisher<out Publisher<out DataBuffer>>): Mono<Void> {
                return delegate.writeAndFlushWith(Flux.from(body)
                    .doOnNext {
                        responseBodyFn(it)
                    })
            }
        }
    } else {
        exchange.response
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
                Channels.newChannel(responseBodyBaos).write(partialBody.asByteBuffer().asReadOnlyBuffer())
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
            String(requestBodyBaos.toByteArray())
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
            String(responseBodyBaos.toByteArray())
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
