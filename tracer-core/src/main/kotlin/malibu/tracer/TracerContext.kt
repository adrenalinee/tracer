package malibu.tracer

import java.util.*

class TracerContext {
    companion object {
        const val TRACE_FIELD_NAME = "trace"
        const val ATTR_REQUEST_CONTEXT = "malibu.tracer.TracerContext.REQUEST_CONTEXT"
        const val ATTR_REQUEST_ERROR = "malibu.tracer.TracerContext.REQUEST_ERROR"

        const val TRACER_LOG_NAME = "TRACER_LOG"
        const val TRACER_DETAIL_LOG_NAME = "TRACER_DETAIL_LOG"

        const val DEFAULT_TRACE_ID_HEADER_NAME = "x-request-id"
        const val DEFAULT_CLIENT_SPAN_ID_HEADER_NAME = "x-parent-span-id"

        const val SHORT_TRACE_ID_LENGTH = 10
    }

    /**
     * application 최초 실행할때 한번만 셋팅된다.
     */
    val runId = UUID.randomUUID().toString().substring(0, 10)

    /**
     * 로그 메시지에 application 을 구분지어줄 이름
     */
    var domain: String? = null

    /**
     * spanId를 사용할지 여부
     */
    var useSpan: Boolean = false

    var traceIdGenerator: TraceIdGenerator<*> = malibu.tracer.SimpleTraceIdGenerator()

    /**
     * useSpan == true 일때만 사용된다.
     */
    var spanIdGenerator: malibu.tracer.SpanIdGenerator<*> = malibu.tracer.SimpleSpanIdGenerator()

}