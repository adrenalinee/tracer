package malibu.tracer.webmvc

import malibu.tracer.TraceIdGenerator
import malibu.tracer.TracerContext
import java.util.*

class StdWebMvcTraceIdGenerator(
    val traceIdHeaderName: String = TracerContext.DEFAULT_TRACE_ID_HEADER_NAME
): TraceIdGenerator<ServletExchange> {

//    private val logger = KotlinLogging.logger {}

    override fun generate(servletExchange: ServletExchange): String {
        return servletExchange.request.getHeaderValueOrNull(traceIdHeaderName)
            ?:  UUID.randomUUID().toString()
    }
}