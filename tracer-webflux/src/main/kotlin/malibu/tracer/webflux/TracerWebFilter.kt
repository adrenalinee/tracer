package malibu.tracer.webflux

import malibu.tracer.*
import malibu.tracer.io.RequestHttpLog
import malibu.tracer.io.ResponseHttpLog
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicBoolean

class TracerWebFilter(
    private val tracerLogger: TracerLogger,
    private val tracerContext: TracerContext,
    private val tracerWebfluxContext: TracerWebfluxContext
): WebFilter {
//    companion object {
//        private val traceDataCreatorRegistry = TraceDataCreatorRegistry.getInstance()
//    }

    private val logger = KotlinLogging.logger {}

    private data class ResponseLoggingState(
        val emitted: AtomicBoolean = AtomicBoolean(false)
    )

    /**
     *
     */
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (logger.isDebugEnabled) {
            logger.debug { "start" }
        }

        val request = exchange.request
        val path = request.uri.path
        val requestLoggingEnabled = tracerWebfluxContext.shouldTraceRequest(request)
        val responseLoggingEnabled = tracerWebfluxContext.shouldTraceResponse(request)

        if (requestLoggingEnabled.not() && responseLoggingEnabled.not()) {
            logger.debug { "trace logging skipped by predicates. path: $path" }

            return chain.filter(exchange)
                .contextWrite { context ->
                    context.put(
                        TracerContext.ATTR_REQUEST_CONTEXT,
                        malibu.tracer.EachRequestContext(request.uri.toString(), path, request.method.name(), false, null)
                    )
                }
        }

        val traceSpanId = createTraceSpanId(exchange)
        val requestContext = malibu.tracer.EachRequestContext(request.uri.toString(), path, request.method.name(), true, traceSpanId)

        exchange.attributes.put(TracerContext.ATTR_REQUEST_CONTEXT, requestContext)
        exchange.attributes.put(ServerWebExchange.LOG_ID_ATTRIBUTE, traceSpanId.shortTraceId)

        val tracerExchange = TracerServerWebExchange(
            exchange = exchange,
            tracerWebfluxContext = tracerWebfluxContext,
            requestLoggingEnabled = requestLoggingEnabled,
            responseLoggingEnabled = responseLoggingEnabled
        )
        tracerExchange.onRequestBodyReadComplete = {
            requestLogging(traceSpanId, tracerExchange, requestLoggingEnabled)
        }
        val responseLoggingState = ResponseLoggingState()
        tracerExchange.onResponseWriteComplete = {
            responseLogging(traceSpanId, tracerExchange, responseLoggingEnabled, responseLoggingState)
        }
        tracerExchange.onResponseSseEvent = { event ->
            sseEventLogging(traceSpanId, tracerExchange, responseLoggingEnabled, event)
        }

        initExchange(tracerExchange, traceSpanId, requestLoggingEnabled)

        return chain.filter(tracerExchange)
            .contextWrite { context ->
                context.put(TracerContext.ATTR_REQUEST_CONTEXT, requestContext)
            }
//            .subscriberContext { context ->
//                context.put(TracerContext.ATTR_REQUEST_CONTEXT, requestContext)
//            }
    }

    /**
     *
     */
    private fun createTraceSpanId(exchange: ServerWebExchange): TraceSpanId {
        val traceId = (tracerContext.traceIdGenerator as TraceIdGenerator<ServerWebExchange>).generate(exchange)
        val shortTraceId = traceId.substring(0, minOf(traceId.length, TracerContext.SHORT_TRACE_ID_LENGTH))
        val spanId = if (tracerContext.useSpan) {
            (tracerContext.spanIdGenerator as malibu.tracer.SpanIdGenerator<ServerWebExchange>).generate(exchange)
        } else {
            null
        }

        return TraceSpanId(traceId, shortTraceId, spanId)
    }

    /**
     * request log, response log 를 출력하기 위한 event handler 등록
     */
    private fun initExchange(exchange: TracerServerWebExchange, traceSpanId: TraceSpanId, requestLoggingEnabled: Boolean) {
        if (exchange.request.headers.contentLength <= 0 ||
            tracerWebfluxContext.traceRequestBody.not()) {
            //request body 가 없는 경우 or request body 를 로깅하지 않는 경우 request 로그 출력
            requestLogging(traceSpanId, exchange, requestLoggingEnabled)
        }
    }

    private fun requestLogging(traceSpanId: TraceSpanId, exchange: TracerServerWebExchange, requestLoggingEnabled: Boolean) {
        if (requestLoggingEnabled.not()) {
            return
        }

        if (tracerLogger.isInforEnabled()) {
            val requestHttpLog = createRequestLog(exchange)
            tracerLogger.infor(
                requestHttpLog,
                "HTTP request: ${exchange.request.method} ${exchange.request.uri}",
                exchange,
                traceSpanId
            )

            if (tracerLogger.isDebugDetailEnabled()) {
                val body = if (tracerWebfluxContext.traceRequestBody) {
                    requestHttpLog.body
                } else {
                    "[warn: request body not tracing!!]"
                }
                tracerLogger.deDat(requestHttpLog, "HTTP request body: $body", traceSpanId)
            }
        }
    }

    private fun responseLogging(
        traceSpanId: TraceSpanId,
        exchange: TracerServerWebExchange,
        responseLoggingEnabled: Boolean,
        state: ResponseLoggingState
    ) {
        if (responseLoggingEnabled.not()) {
            return
        }

        if (state.emitted.compareAndSet(false, true).not()) {
            return
        }

        if (tracerLogger.isInforEnabled()) {
            val throwable = exchange.getAttribute<Throwable>(TracerContext.ATTR_REQUEST_ERROR)
            val responseHttpLog = createResponseLog(exchange, throwable)
            val responseStatus = responseHttpLog.status ?: HttpStatus.OK.value()

            if (throwable != null || responseStatus >= HttpStatus.BAD_REQUEST.value()) {
                tracerLogger.error(
                    responseHttpLog,
                    "HTTP response: ${exchange.response.statusCode ?: responseStatus} ${throwable ?: ""}",
                    throwable,
                    exchange,
                    traceSpanId
                )
            } else {
                tracerLogger.infor(
                    responseHttpLog,
                    "HTTP response: ${exchange.response.statusCode ?: responseStatus}",
                    exchange,
                    traceSpanId
                )

                if (tracerLogger.isDebugDetailEnabled()) {
                    val body = if (tracerWebfluxContext.traceResponseBody) {
                        responseHttpLog.body
                    } else {
                        "[warn: response body not tracing!!]"
                    }
                    tracerLogger.deDat(responseHttpLog, "HTTP response body: $body", traceSpanId)
                }
            }
        }
    }

    private fun sseEventLogging(
        traceSpanId: TraceSpanId,
        exchange: TracerServerWebExchange,
        responseLoggingEnabled: Boolean,
        event: String
    ) {
        if (responseLoggingEnabled.not()) {
            return
        }

        if (tracerLogger.isDebugDetailEnabled().not()) {
            return
        }

        tracerLogger.deDat(
            createResponseLog(exchange, null, event),
            "HTTP response sse event: $event",
            traceSpanId
        )
    }

    /**
     *
     */
    private fun createRequestLog(exchange: TracerServerWebExchange): RequestHttpLog {
//        println(request.id)
//        println(request.remoteAddress?.hostName)
//        println(request.remoteAddress?.port)
//
//        println(request.localAddress?.hostName)
//        println(request.localAddress?.port)

        return RequestHttpLog(
            method = exchange.request.method.toString(),
            url = exchange.request.uri.toString(),
            path = exchange.request.uri.path,
            headers = if (tracerWebfluxContext.traceRequestHeaders) {
                exchange.request.headers.let { headers ->
                    LinkedMultiValueMap<String, String>().also { map ->
                        headers.forEach { name, values ->
                            map.put(name, values)
                        }
                    }
                }
            } else {
                null
            },
            body = if (tracerWebfluxContext.traceRequestBody) {
                exchange.genRequestBody()
            } else {
                null
            }
        )
    }

    /**
     *
     */
    private fun createResponseLog(
        exchange: TracerServerWebExchange,
        throwable: Throwable?,
        body: String? = if (tracerWebfluxContext.traceResponseBody) exchange.genResponseBody() else null
    ): ResponseHttpLog {
        return ResponseHttpLog(
            method = exchange.request.method.toString(),
            status = exchange.response.statusCode?.value(),
            url = exchange.request.uri.toString(),
            path = exchange.request.uri.path,
            elapsedTime = System.currentTimeMillis() - exchange.startedTime,
            headers = if (tracerWebfluxContext.traceResponseHeaders) {
                exchange.response.headers.toMultiValueMap()
            } else {
                null
            },
            body = body,
            error = if (tracerWebfluxContext.tracedError) {
                throwable?.traceToString()
            } else {
                throwable?.toString()
            }
        )
    }
}
