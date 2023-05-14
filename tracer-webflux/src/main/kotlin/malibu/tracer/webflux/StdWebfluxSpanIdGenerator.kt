package malibu.tracer.webflux

import malibu.tracer.SpanIdGenerator
import malibu.tracer.TracerContext
import mu.KotlinLogging
import org.springframework.web.server.ServerWebExchange

class StdWebfluxSpanIdGenerator(
    val spanIdHeaderName: String = TracerContext.DEFAULT_CLIENT_SPAN_ID_HEADER_NAME
): malibu.tracer.SpanIdGenerator<ServerWebExchange> {
    private val logger = KotlinLogging.logger {}

    override fun generate(logContext: ServerWebExchange): Int {
        return logContext.request.headers
            .getFirst(spanIdHeaderName)
            ?.let { prevSpanId ->
                try {
                    prevSpanId.toInt() + 1
                } catch (ex: NumberFormatException) {
                    logger.warn {
                        "${TracerContext.DEFAULT_CLIENT_SPAN_ID_HEADER_NAME} header value가 숫자가 아닙니다. " +
                                "${TracerContext.DEFAULT_CLIENT_SPAN_ID_HEADER_NAME}: $prevSpanId"
                    }
                    null
                }
            }
            ?: 1
    }
}