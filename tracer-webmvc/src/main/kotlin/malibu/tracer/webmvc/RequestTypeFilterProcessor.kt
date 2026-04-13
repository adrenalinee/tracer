package malibu.tracer.webmvc

import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import malibu.tracer.TraceSpanId
import malibu.tracer.TracerContext
import malibu.tracer.TracerLogger
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler
import java.util.concurrent.atomic.AtomicBoolean

class RequestTypeFilterProcessor(
    private val tracerLogger: TracerLogger,
    private val tracerWebMvcContext: TracerWebMvcContext
) {

    private val logger = KotlinLogging.logger {}

    fun doFilter(
        traceSpanId: TraceSpanId,
        servletExchange: ServletExchange,
        filterChain: FilterChain,
        requestLoggingEnabled: Boolean,
        responseLoggingEnabled: Boolean
    ) {
        logger.debug { "start" }

        val request = servletExchange.request
        val response = servletExchange.response

        if (request.dispatcherType != DispatcherType.REQUEST) {
            throw RuntimeException("dispatcherType 이 REQUEST 가 아닙니다. dispatcherType: ${request.dispatcherType}")
        }

        if (requestLoggingEnabled &&
            (tracerWebMvcContext.traceRequestBody.not() || request.contentLength <= 0)) {
            requestLogging(traceSpanId, servletExchange)
        }

        try {
            filterChain.doFilter(request, response)

            if (requestLoggingEnabled &&
                tracerWebMvcContext.traceRequestBody &&
                request.contentLength > 0) {
                requestLogging(traceSpanId, servletExchange)
            }

            if (request.isAsyncStarted) {
                registerAsyncResponseLogging(traceSpanId, servletExchange, responseLoggingEnabled)
            } else {
                if (shouldLogResponse(request, responseLoggingEnabled)) {
                    responseLogging(traceSpanId, servletExchange)
                }
                servletExchange.notifyResponseComplete()
            }
        } catch (ex: Exception) {
            request.setAttribute(TracerContext.ATTR_REQUEST_ERROR, ex)

            if (requestLoggingEnabled &&
                tracerWebMvcContext.traceRequestBody &&
                request.contentLength > 0) {
                requestLogging(traceSpanId, servletExchange)
            }

            servletExchange.notifyResponseComplete()
            throw ex
        }
    }

    private fun requestLogging(traceSpanId: TraceSpanId, servletExchange: ServletExchange) {
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
        if (markResponseLogged(servletExchange).not()) {
            return
        }

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

    private fun registerAsyncResponseLogging(
        traceSpanId: TraceSpanId,
        servletExchange: ServletExchange,
        responseLoggingEnabled: Boolean
    ) {
        val request = servletExchange.request
        val listener = object : AsyncListener {
            override fun onComplete(event: AsyncEvent) {
                if (shouldLogResponse(request, responseLoggingEnabled)) {
                    responseLogging(traceSpanId, servletExchange)
                }
                servletExchange.notifyResponseComplete()
            }

            override fun onTimeout(event: AsyncEvent) {}

            override fun onError(event: AsyncEvent) {
                event.throwable?.also { throwable ->
                    request.setAttribute(TracerContext.ATTR_REQUEST_ERROR, throwable)
                }
            }

            override fun onStartAsync(event: AsyncEvent) {
                event.asyncContext.addListener(this)
            }
        }

        request.asyncContext.addListener(listener)
    }

    private fun shouldLogResponse(
        request: jakarta.servlet.http.HttpServletRequest,
        responseLoggingEnabled: Boolean
    ): Boolean {
        if (responseLoggingEnabled.not()) {
            return false
        }

        if (request.getAttributeOrNull<Any>(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE) is ResourceHttpRequestHandler) {
            return false
        }

        return request.getAttributeOrNull<Any>(DispatcherServlet.EXCEPTION_ATTRIBUTE)?.let { error ->
            (error is HttpRequestMethodNotSupportedException ||
                error is HttpMediaTypeNotSupportedException).not()
        } != false
    }

    private fun markResponseLogged(servletExchange: ServletExchange): Boolean {
        val state = servletExchange.request.getAttribute(TracerFilter.ATTR_RESPONSE_LOGGED) as? AtomicBoolean
            ?: AtomicBoolean(false).also {
                servletExchange.request.setAttribute(TracerFilter.ATTR_RESPONSE_LOGGED, it)
            }

        return state.compareAndSet(false, true)
    }
}
