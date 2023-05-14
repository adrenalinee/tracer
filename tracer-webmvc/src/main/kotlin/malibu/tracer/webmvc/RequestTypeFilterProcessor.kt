package malibu.tracer.webmvc

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerContext
import malibu.tracer.TracerLogger
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler
import org.springframework.web.util.ContentCachingResponseWrapper

class RequestTypeFilterProcessor(
    private val tracerLogger: TracerLogger,
    private val tracerWebMvcContext: TracerWebMvcContext
) {

    private val logger = KotlinLogging.logger {}
//    private val logger2 = KotlinLogging.logger("TRACER_DETAIL")

//    private val traceDataCreatorRegistry = TraceDataCreatorRegistry.getInstance()

    fun doFilter(
        traceSpanId: TraceSpanId,
        servletExchange: ServletExchange,
        filterChain: FilterChain
    ) {
        logger.debug { "start" }

        val request = servletExchange.request
        val response = servletExchange.response

        if (request.dispatcherType != DispatcherType.REQUEST) {
            throw RuntimeException("dispatcherType 이 REQUEST 가 아닙니다. dispatcherType: ${request.dispatcherType}")
        }

        //바디 출력할 필요없을때 여기서 request 로그 출력
        if (tracerWebMvcContext.traceRequestBody.not() ||
            request.contentLength <= 0) {
            requestLogging(traceSpanId, servletExchange)
        }

        try {
            filterChain.doFilter(request, response)

//            for (attributeName in request.attributeNames) {
//                println("$attributeName: ${request.getAttribute(attributeName)}")
//            }

//            val v = request.getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingHandler")
//            println(v)
//            println(v.javaClass)

//            if (request.getAttributeOrNull<Any>(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE) is ResourceHttpRequestHandler) {
//
//            }

//            if (response.status >= HttpStatus.BAD_REQUEST.value()) {
//                //@ExceptionHandler 로 처리할 경우에 여기서 처리됨.
//                //404 등 trhow exception 없이 에러 처리되는 경우도 여기에 해당하나 그 경우에는
//                //EXCEPTION_ATTRIBUTE 가 비어 있음.
//                request.getAttributeOrNull<Exception>(DispatcherServlet.EXCEPTION_ATTRIBUTE)
//                    ?.also { error -> throw error }
//            }

            //정상처리되었을때, 바디 출력해야 될때 여기서 request 로그 출력
            if (tracerWebMvcContext.traceRequestBody && request.contentLength > 0) {
                requestLogging(traceSpanId, servletExchange)
            }

            request.getAttributeOrNull<Any>(DispatcherServlet.EXCEPTION_ATTRIBUTE)

//            if (response.status < HttpStatus.BAD_REQUEST.value()) {
            if ((request.getAttributeOrNull<Any>(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE) is ResourceHttpRequestHandler).not() &&
                request.getAttributeOrNull<Any>(DispatcherServlet.EXCEPTION_ATTRIBUTE)?.let { error ->
                    (error is HttpRequestMethodNotSupportedException ||
                            error is HttpMediaTypeNotSupportedException).not()
                } != false) {
                //정상처리일때 response 로그 출력
                responseLogging(traceSpanId, servletExchange)
            }

            if (response is ContentCachingResponseWrapper) {
                response.copyBodyToResponse()
            }
        } catch (ex: Exception) {
            request.setAttribute(TracerContext.ATTR_REQUEST_ERROR, ex)

            //에러 상황일때, 바디 출력할 필요없을때 request 로그 출력
            if (tracerWebMvcContext.traceRequestBody &&
                request.contentLength > 0) {
                requestLogging(traceSpanId, servletExchange)
            }

            throw ex
        }
    }

    private fun requestLogging(traceSpanId: TraceSpanId, servletExchange: ServletExchange) {
//        val requestHttpLog = takeIf { tracerLogger.isInfoEnabled() }
//            ?.let {
//                val requestHttpLog = createRequestLog(tracerWebMvcContext, servletExchange)
//                tracerLogger.info(
//                    traceSpanId,
//                    requestHttpLog,
//                    traceDataCreatorRegistry.traceLogType(DefinedTraceLogType.REQUEST)
//                        .create(DefinedProtocolType.HTTP, servletExchange),
//                    "HTTP request: ${servletExchange.request.method} ${servletExchange.request.getUrl()}"
//                )
//                requestHttpLog
//            }



        if (tracerLogger.isInforEnabled()) {
            val requestHttpLog = createRequestLog(tracerWebMvcContext, servletExchange)
            tracerLogger.infor(
                requestHttpLog,
                "HTTP request: ${servletExchange.request.method} ${servletExchange.request.getUrl()}",
                servletExchange,
                traceSpanId
            )

            if (tracerLogger.isDebugDetailEnabled()) {
                val body = if (tracerWebMvcContext.traceRequestBody) {
                    requestHttpLog.body
                } else {
                    "[warn: request body not tracing!!]"
                }
                tracerLogger.deDat(requestHttpLog, "HTTP request body: $body", traceSpanId)
            }
        }
    }

    private fun responseLogging(traceSpanId: TraceSpanId, servletExchange: ServletExchange) {
        if (tracerLogger.isInforEnabled()) {
            val responseHttpLog = createResponseLog(tracerWebMvcContext, servletExchange, null)
            tracerLogger.infor(
                responseHttpLog,
                "HTTP response: ${servletExchange.response.status} ${HttpStatus.resolve(servletExchange.response.status)?.reasonPhrase}",
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
        }
    }
}
