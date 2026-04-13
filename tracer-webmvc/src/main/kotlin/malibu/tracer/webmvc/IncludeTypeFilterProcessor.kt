package malibu.tracer.webmvc

import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import java.util.concurrent.atomic.AtomicBoolean

class IncludeTypeFilterProcessor(
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
        if (request.dispatcherType != DispatcherType.INCLUDE) {
            throw RuntimeException("dispatcherType이 INCLUDE가 아닙니다. dispatcherType: ${request.dispatcherType}")
        }

        if (responseLoggingEnabled.not()) {
            return
        }

        val state = request.getAttribute(TracerFilter.ATTR_RESPONSE_LOGGED) as? AtomicBoolean
            ?: AtomicBoolean(false).also {
                request.setAttribute(TracerFilter.ATTR_RESPONSE_LOGGED, it)
            }
        if (state.compareAndSet(false, true).not()) {
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
}
