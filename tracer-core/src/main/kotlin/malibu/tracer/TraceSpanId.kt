package malibu.tracer

data class TraceSpanId(
    val traceId: String,
    val shortTraceId: String,
    val spanId: Int?
)
