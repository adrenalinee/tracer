package malibu.tracer.webmvc

import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import malibu.tracer.TraceSpanId
import malibu.tracer.TracerContext
import malibu.tracer.TracerLogger
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import java.util.concurrent.atomic.AtomicBoolean

class ErrorTypeFilterProcessor(
    private val tracerLogger: TracerLogger,
    private val tracerWebMvcContext: TracerWebMvcContext
) {

    private val logger = KotlinLogging.logger {}

    fun doFilter(
        traceSpanId: TraceSpanId,
        servletExchange: ServletExchange,
        filterChain: FilterChain,
        responseLoggingEnabled: Boolean
    ) {
        logger.debug { "start" }

        val request = servletExchange.request
        val response = servletExchange.response

        if (request.dispatcherType != DispatcherType.ERROR) {
            throw RuntimeException("dispatcherType이 ERROR가 아닙니다. dispatcherType: ${request.dispatcherType}")
        }

        filterChain.doFilter(request, response)

        if (responseLoggingEnabled.not()) {
            servletExchange.notifyResponseComplete()
            return
        }

        val state = request.getAttribute(TracerFilter.ATTR_RESPONSE_LOGGED) as? AtomicBoolean
            ?: AtomicBoolean(false).also {
                request.setAttribute(TracerFilter.ATTR_RESPONSE_LOGGED, it)
            }
        if (state.compareAndSet(false, true).not()) {
            servletExchange.notifyResponseComplete()
            return
        }

        val method = RequestContextHolder.context().method
        val url = RequestContextHolder.context().url
        val path = RequestContextHolder.context().path
        val throwable = request.getAttribute(TracerContext.ATTR_REQUEST_ERROR) as Throwable?

        val responseHttpLog = createResponseLog(tracerWebMvcContext, servletExchange, throwable, method, path, url)
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

        servletExchange.notifyResponseComplete()
    }
}
