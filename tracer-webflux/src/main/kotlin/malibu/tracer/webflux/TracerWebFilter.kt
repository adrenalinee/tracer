package malibu.tracer.webflux

import malibu.tracer.*
import malibu.tracer.io.RequestHttpLog
import malibu.tracer.io.ResponseHttpLog
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class TracerWebFilter(
    private val tracerLogger: TracerLogger,
    private val tracerContext: TracerContext,
    private val tracerWebfluxContext: TracerWebfluxContext
): WebFilter {
//    companion object {
//        private val traceDataCreatorRegistry = TraceDataCreatorRegistry.getInstance()
//    }

    private val logger = KotlinLogging.logger {}

    private val antPathMatcher = AntPathMatcher()

    /**
     *
     */
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (logger.isDebugEnabled) {
            logger.debug { "start" }
        }

        val path = exchange.request.uri.path
        if (tracerWebfluxContext.excludePathPaterns.any { pathPattern ->
            antPathMatcher.match(pathPattern, path)
        }) { //exclude path 에 속한 경로로 들어온 요청이라면 로깅안함.
            logger.debug { "exclude path matched! trace logging skip. path: $path" }

            return chain.filter(exchange)
                .contextWrite { context ->
                    context.put(
                        TracerContext.ATTR_REQUEST_CONTEXT,
                        malibu.tracer.EachRequestContext(exchange.request.uri.toString(), path, false, null)
                    )
                }
//                .subscriberContext { context ->
//                    context.put(TracerContext.ATTR_REQUEST_CONTEXT, EachRequestContext(exchange.request.uri.toString(), path, false, null))
//                }
        }

        val traceSpanId = createTraceSpanId(exchange)
        val requestContext = malibu.tracer.EachRequestContext(exchange.request.uri.toString(), path, true, traceSpanId)

        exchange.attributes.put(TracerContext.ATTR_REQUEST_CONTEXT, requestContext)
        exchange.attributes.put(ServerWebExchange.LOG_ID_ATTRIBUTE, traceSpanId.shortTraceId)

        val tracerExchange = TracerServerWebExchange(exchange, tracerWebfluxContext)
        tracerExchange.onRequestBodyReadComplete = {
            //request body가 있는 경우 && request body를 로깅하는 경우
            requestLogging(traceSpanId, tracerExchange)
        }

        initExchange(tracerExchange, traceSpanId)

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
    private fun initExchange(exchange: TracerServerWebExchange, traceSpanId: TraceSpanId) {
        if (exchange.request.headers.contentLength <= 0 ||
            tracerWebfluxContext.traceRequestBody.not()) {
            //request body 가 없는 경우 or request body 를 로깅하지 않는 경우 request 로그 출력
            requestLogging(traceSpanId, exchange)
        }

        exchange.response.beforeCommit {
            if (exchange.request.headers.contentLength > 0 &&
                tracerWebfluxContext.traceRequestBody &&
                exchange.isReadRequestBody.not()
            ) {
                //TODO controller가 body를 읽지 않았을 경우에 여기서 읽음. subscribe()가 완료되면 미리셋팅된 이벤트 발생으로 로그가 출력된다.
                //TODO body가 너무 클 경우를 대비해서 일정 크기까지만 읽어야 한다.
            }

            //response 로그 출력
            val throwable = exchange.getAttribute<Throwable?>(TracerContext.ATTR_REQUEST_ERROR)
            if (throwable != null || exchange.response.statusCode!!.value() >= HttpStatus.BAD_REQUEST.value()) {
                //error 상황 response 로그 출력
                tracerLogger.error(
                    createResponseLog(exchange, throwable),
                    "HTTP response: ${exchange.response.statusCode} ${throwable?: ""}",
                    throwable,
                    exchange,
                    traceSpanId
                )
            } else {
                //정상상황 response 로그 출력
                responseLogging(traceSpanId, exchange)
            }

            Mono.empty()
        }
    }

    private fun requestLogging(traceSpanId: TraceSpanId, exchange: TracerServerWebExchange) {
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

    private fun responseLogging(traceSpanId: TraceSpanId, exchange: TracerServerWebExchange) {
        if (tracerLogger.isInforEnabled()) {
            val responsetHttpLog = createResponseLog(exchange, null)
            tracerLogger.infor(
                responsetHttpLog,
                "HTTP response: ${exchange.response.statusCode}",
                exchange,
                traceSpanId
            )

            if (tracerLogger.isDebugDetailEnabled()) {
                val body = if (tracerWebfluxContext.traceResponseBody) {
                    responsetHttpLog.body
                } else {
                    "[warn: response body not tracing!!]"
                }
                tracerLogger.deDat(responsetHttpLog, "HTTP response body: $body", traceSpanId)
            }
        }
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
                exchange.request.headers
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
    private fun createResponseLog(exchange: TracerServerWebExchange, throwable: Throwable?): ResponseHttpLog {
        return ResponseHttpLog(
            method = exchange.request.method.toString(),
            status = exchange.response.rawStatusCode,
            url = exchange.request.uri.toString(),
            path = exchange.request.uri.path,
            elapsedTime = System.currentTimeMillis() - exchange.startedTime,
            headers = if (tracerWebfluxContext.traceResponseHeaders) {
                exchange.response.headers
            } else {
                null
            },
            body = if (tracerWebfluxContext.traceResponseBody) {
                exchange.genResponseBody()
            } else {
                null
            },
            error = if (tracerWebfluxContext.tracedError) {
                throwable?.traceToString()
            } else {
                throwable?.toString()
            }
        )
    }
}