package malibu.tracer.webmvc

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerContext
import malibu.tracer.TracerLogger
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.util.ContentCachingResponseWrapper

class ErrorTypeFilterProcessor(
    private val tracerLogger: TracerLogger,
    private val tracerWebMvcContext: TracerWebMvcContext
) {

    private val logger = KotlinLogging.logger {}

//    private val traceDataCreatorRegistry = TraceDataCreatorRegistry.getInstance()

    fun doFilter(
//        request: HttpServletRequest,
//        response: HttpServletResponse,
        traceSpanId: TraceSpanId,
        servletExchange: ServletExchange,
        filterChain: FilterChain
    ) {
        logger.debug { "start" }

        val request = servletExchange.request
        val response = servletExchange.response

        if (request.dispatcherType != DispatcherType.ERROR) {
            throw RuntimeException("dispatcherType이 ERROR가 아닙니다. dispatcherType: ${request.dispatcherType}")
        }

//        val arrangedRequest = if (tracerWebMvcContext.traceRequestBody) {
//            ContentCachingRequestWrapper(request)
//        } else {
//            request
//        }
//
//        val arrangedResponse = if (tracerWebMvcContext.traceResponseBody) {
//            ContentCachingResponseWrapper(response)
//        } else {
//            response
//        }

//        val servletExchange = ServletExchange(arrangedRequest, arrangedResponse)
//        val traceSpanId = RequestContextHolder.context().traceSpanId

        filterChain.doFilter(request, response)

        val url = RequestContextHolder.context().url
        val path = RequestContextHolder.context().path
//        val throwable = RequestContextHolder.context().getError()
        val throwable = request.getAttribute(TracerContext.ATTR_REQUEST_ERROR) as Throwable?

        //에러처리일때 response 로그 출력
        val responseHttpLog = createResponseLog(tracerWebMvcContext, servletExchange, throwable, path, url)
        tracerLogger.error(
            responseHttpLog,
            "HTTP response: ${response.status} ${HttpStatus.resolve(response.status)?.reasonPhrase}",
            throwable,
            servletExchange,
            traceSpanId
        )

        if (tracerLogger.isDebugDetailEnabled()) {
            val body = if (tracerWebMvcContext.traceResponseBody) {
                responseHttpLog.body
            } else {
                "[warn: response body not tracing!!]"
            }
            tracerLogger.deDat(responseHttpLog, "HTTP response body: $body", traceSpanId)
        }

        if (response is ContentCachingResponseWrapper) {
            response.copyBodyToResponse()
        }
    }
}