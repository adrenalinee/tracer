package malibu.tracer.webmvc

import malibu.tracer.*
import malibu.tracer.io.RequestHttpLog
import malibu.tracer.io.ResponseHttpLog
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

/**
 * 참고:
 *  - https://leeyongjin.tistory.com/entry/request-response-logging
 *  - https://hirlawldo.tistory.com/44
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class TracerFilter(
    private val tracerLogger: TracerLogger,
    private val tracerContext: TracerContext,
    private val tracerWebMvcContext: TracerWebMvcContext
): OncePerRequestFilter() {

    private val logger2 = KotlinLogging.logger {}

    private val antPathMatcher = AntPathMatcher()

    private val requestTypeFilterProcessor = RequestTypeFilterProcessor(tracerLogger, tracerWebMvcContext)

    private val errorTypeFilterProcessor = ErrorTypeFilterProcessor(tracerLogger, tracerWebMvcContext)

    private val includeTypeFilterProcessor = IncludeTypeFilterProcessor(tracerLogger, tracerWebMvcContext)


    /**
     * true - error 처리를 위해 forwarding 되었을때도 filter를 사용하게 해준다.
     */
    override fun shouldNotFilterErrorDispatch(): Boolean {
        return false
    }

    /**
     * 현재 요청에 대해서 filter를 적용할지 말지 결정.
     * exclude path에 포함되어 있다면 로깅을 하지 않도록 처리.
     */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = when (request.dispatcherType) {
            DispatcherType.REQUEST -> request.requestURI
            DispatcherType.ERROR -> request.getAttribute("jakarta.servlet.error.request_uri") as String //forwarding 되기전 원래의 path..
            DispatcherType.INCLUDE -> {
//                for (attributeName in request.attributeNames) {
//                    println("$attributeName: ${request.getAttribute(attributeName)}")
//                }
//
//                println(request.requestURI)
                request.requestURI
            }
            else -> {
                logger2.warn { "tracer에서 지원하지 않는 request dispatcherType입니다. dispatcherType: ${request.dispatcherType}" }
                return false
            }
        }

        if (tracerWebMvcContext.excludePathPaterns.any { pathPattern ->
                antPathMatcher.match(pathPattern, path)
            }) {
            logger2.debug { "exclude path matched! trace logging skip. path: $path" }


            request.setAttribute(TracerContext.ATTR_REQUEST_CONTEXT,
                malibu.tracer.EachRequestContext(request.getUrl(), path, false, null)
            )
            return true
        }

        return false
    }

    /**
     *
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger2.debug { "start" }

        if (logger2.isTraceEnabled) {
            logger2.trace { "request.dispatcherType: ${request.dispatcherType}" }
        }

        val servletExchange = createServletExchange(request, response)
        try {
            when (request.dispatcherType) {
                DispatcherType.REQUEST -> {
//                    val servletExchange = createServletExchange(request, response)
                    val traceSpanId = createTraceSpanId(servletExchange)
                    val requestContext = malibu.tracer.EachRequestContext(
                        url = servletExchange.request.getUrl(),
                        path = request.requestURI,
                        isInclude = true,
                        traceSpanId = traceSpanId
                    )
                    RequestContextHolder.createFrom(requestContext) //request context 등록
                    request.setAttribute(TracerContext.ATTR_REQUEST_CONTEXT, requestContext)
                    request.setAttribute(ServerWebExchange.LOG_ID_ATTRIBUTE, traceSpanId.shortTraceId)

                    requestTypeFilterProcessor.doFilter(traceSpanId, servletExchange, filterChain)
                }
                DispatcherType.INCLUDE -> {
//                    filterChain.doFilter(request, response)

//                    val servletExchange = createServletExchange(request, response)
                    val requestContext = request.getAttribute(TracerContext.ATTR_REQUEST_CONTEXT) as malibu.tracer.EachRequestContext
                    val traceSpanId = requestContext.traceSpanId!!
                    RequestContextHolder.createFrom(requestContext) //request context 등록
                    includeTypeFilterProcessor.doFilter(traceSpanId, servletExchange, filterChain)
//                    if (tracerLogger.isInforEnabled()) {
//                        val responseHttpLog = createResponseLog(tracerWebMvcContext, servletExchange, null)
//                        tracerLogger.infor(
//                            responseHttpLog,
//                            servletExchange,
//                            "HTTP response: ${servletExchange.response.status} ${HttpStatus.resolve(servletExchange.response.status)?.reasonPhrase}",
//                            traceSpanId
//                        )
//
//                        if (tracerLogger.isDebugDetailEnabled()) {
//                            val body = if (tracerWebMvcContext.traceResponseBody) {
//                                responseHttpLog.body
//                            } else {
//                                "[warn: response body not tracing!!]"
//                            }
//                            tracerLogger.deDat(responseHttpLog, "HTTP response body: $body", traceSpanId)
//                        }
//                    }

                }
                DispatcherType.ERROR -> {
//                    val servletExchange = createServletExchange(request, response)
                    val requestContext = request.getAttribute(TracerContext.ATTR_REQUEST_CONTEXT) as malibu.tracer.EachRequestContext
                    val traceSpanId = requestContext.traceSpanId!!
                    RequestContextHolder.createFrom(requestContext) //request context 등록

                    errorTypeFilterProcessor.doFilter(traceSpanId, servletExchange, filterChain)
                }
                else -> {
                    logger2.warn { "tracer에서 지원하지 않는 request dispatcherType입니다. dispatcherType: ${request.dispatcherType}" }
                    filterChain.doFilter(request, response)
                }
            }
        } finally {
            RequestContextHolder.remove() //request context 삭제
        }

    }

    private fun createServletExchange(request: HttpServletRequest, response: HttpServletResponse): ServletExchange {
        val arrangedRequest = if (tracerWebMvcContext.traceRequestBody) {
            ContentCachingRequestWrapper(request)
        } else {
            request
        }

        val arrangedResponse = if (tracerWebMvcContext.traceResponseBody) {
            ContentCachingResponseWrapper(response)
        } else {
            response
        }


        return ServletExchange(arrangedRequest, arrangedResponse)
    }

    private fun createTraceSpanId(servletExchange: ServletExchange): TraceSpanId {
        val traceId = (tracerContext.traceIdGenerator as TraceIdGenerator<ServletExchange>).generate(servletExchange)
        val shortTraceId = traceId.substring(0, minOf(traceId.length, TracerContext.SHORT_TRACE_ID_LENGTH))
        val spanId = if (tracerContext.useSpan) {
            (tracerContext.spanIdGenerator as malibu.tracer.SpanIdGenerator<ServletExchange>).generate(servletExchange)
        } else {
            null
        }

        return TraceSpanId(traceId, shortTraceId, spanId)
    }
}

fun createRequestLog(tracerWebMvcContext: TracerWebMvcContext, servletExchange: ServletExchange): RequestHttpLog {
//        println("requestURI: ${request.requestURI}")
//        println("pathInfo: ${request.pathInfo}")
//        println("contextPath: ${request.contextPath}")
//        println("requestURL: ${request.requestURL}")
//        println("localAddr: ${request.localAddr}")
//        println("remoteAddr: ${request.remoteAddr}")
//        println("remoteHost: ${request.remoteHost}")

    if (tracerWebMvcContext.traceRequestBody &&
        servletExchange.request.inputStream.isFinished.not()) {
        //controller가 body를 읽지 않았을 경우에 여기서 읽음. 읽어야만 contentAsByteArray에 body가 들어 있다.
        //TODO body가 너무 클 경우를 대비해서 일정 크기까지만 읽어야 한다.
        try {
            while (servletExchange.request.inputStream.read() != -1) {}
        } catch(ex: Exception) {
//            logger2.warn("request body 읽는 도중 에러 발생.", ex)
            ex.printStackTrace()
        }
    }

    return RequestHttpLog(
        method = servletExchange.request.method.toString(),
        url = servletExchange.request.getUrl(),
        path = servletExchange.request.getPath(),
        headers = if (tracerWebMvcContext.traceRequestHeaders) { servletExchange.request.getHttpHeaders() } else { null },
        body = if (tracerWebMvcContext.traceRequestBody) {servletExchange.genRequestBody() } else { null }
    )
}

/**
 * @param path - null: request 에서 path 확인, not null: 입력된 값을 path 로 사용. (error type request 일때는 request 의 path 가 원래의 path 가 아니다.)
 */
fun createResponseLog(tracerWebMvcContext: TracerWebMvcContext, servletExchange: ServletExchange, throwable: Throwable?, path: String? = null, url: String? = null): ResponseHttpLog {
    return ResponseHttpLog(
        method = servletExchange.request.method.toString(),
        status = servletExchange.response.status,
        url = url?.let { it }?: servletExchange.request.getUrl(),
        path = path?: servletExchange.request.getPath(),
        elapsedTime = System.currentTimeMillis() - servletExchange.startedTime,
        headers = if (tracerWebMvcContext.traceResponseHeaders) { servletExchange.response.getHttpHeaders() } else { null },
        body = if (tracerWebMvcContext.traceResponseBody) { servletExchange.genResponseBody() } else { null },
        error = if (tracerWebMvcContext.tracedError) {
            throwable?.traceToString()
        } else {
            throwable?.toString()
        }
    )
}