package malibu.tracer.webmvc

import malibu.tracer.EachRequestContext
import malibu.tracer.TraceSpanId
import malibu.tracer.TracerLogger
import malibu.tracer.io.TraceLog

class WebMvcTracerLogger(
    private val tracerLogger: TracerLogger
) {

    fun isDebugEnabled(): Boolean {
        return tracerLogger.isDebugEnabled()
    }

    fun isInfoEnabled(): Boolean {
        return tracerLogger.isInforEnabled()
    }

    fun debug(traceLog: TraceLog, message: String) {
        if (isIncludedRequest()) {
            tracerLogger.debug(traceLog, message, null, getTraceSpanId())
        }
    }

    fun info(traceLog: TraceLog, message: String) {
        if (isIncludedRequest()) {
            tracerLogger.infor(traceLog, message, null, getTraceSpanId())
        }
    }

    fun error(traceLog: TraceLog, message: String) {
        if (isIncludedRequest()) {
            tracerLogger.error(traceLog, message, null, null, getTraceSpanId())
        }
    }

    fun getRunId(): String {
        return tracerLogger.getRunId()
    }

    fun getTraceSpanId(): TraceSpanId? {
        return RequestContextHolder.contextOrNull()?.let { it.traceSpanId }
    }

    fun isIncludedRequest(): Boolean {
        return RequestContextHolder.contextOrNull()
            ?.let { it.isInclude }
            ?: false
    }

    fun getRequestContext(): malibu.tracer.EachRequestContext? {
        return RequestContextHolder.contextOrNull()
    }
}