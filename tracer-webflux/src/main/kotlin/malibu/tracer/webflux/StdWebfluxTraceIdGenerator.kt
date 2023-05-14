package malibu.tracer.webflux

import malibu.tracer.TraceIdGenerator
import malibu.tracer.TracerContext
import org.springframework.web.server.ServerWebExchange
import java.util.*

class StdWebfluxTraceIdGenerator(
    val traceIdHeaderName: String = TracerContext.DEFAULT_TRACE_ID_HEADER_NAME
): TraceIdGenerator<ServerWebExchange> {

    override fun generate(serverWebExchange: ServerWebExchange): String {
        return serverWebExchange.request.headers
            .getFirst(traceIdHeaderName)
            ?: UUID.randomUUID().toString()
    }
}