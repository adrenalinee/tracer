package malibu.tracer.webmvc

import malibu.tracer.TracerContext
import mu.KotlinLogging

class StdWebMvcSpanIdGenerator(
    val spanIdHeaderName: String = TracerContext.DEFAULT_CLIENT_SPAN_ID_HEADER_NAME
): malibu.tracer.SpanIdGenerator<ServletExchange> {
    private val logger = KotlinLogging.logger {}

    override fun generate(logContext: ServletExchange): Int {
        return logContext.request
            .getHeaderValueOrNull(spanIdHeaderName)
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