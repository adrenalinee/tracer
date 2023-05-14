package malibu.tracer.webmvc

import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import mu.KotlinLogging
import org.springframework.http.HttpStatus

class IncludeTypeFilterProcessor(
    private val tracerLogger: TracerLogger,
    private val tracerWebMvcContext: TracerWebMvcContext
) {

    private val logger = KotlinLogging.logger {}


    fun doFilter(
        traceSpanId: TraceSpanId,
        servletExchange: ServletExchange,
        filterChain: FilterChain
    ) {
        logger.debug { "start" }

        val request = servletExchange.request
        if (request.dispatcherType != DispatcherType.INCLUDE) {
            throw RuntimeException("dispatcherType이 INCLUDE가 아닙니다. dispatcherType: ${request.dispatcherType}")
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