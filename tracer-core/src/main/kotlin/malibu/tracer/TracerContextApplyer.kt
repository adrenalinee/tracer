package malibu.tracer

class TracerContextApplyer(
    var domain: String? = null,
    var useSpan: Boolean? = null,
    var traceIdGenerator: TraceIdGenerator<*>? = null,
    var spanIdGenerator: malibu.tracer.SpanIdGenerator<*>? = null
) {

    internal fun apply(tracerContext: TracerContext)  {
        domain?.also { tracerContext.domain = it }
        useSpan?.also { tracerContext.useSpan = it }
        traceIdGenerator?.also { tracerContext.traceIdGenerator = it }
        spanIdGenerator?.also { tracerContext.spanIdGenerator = it }
    }
}