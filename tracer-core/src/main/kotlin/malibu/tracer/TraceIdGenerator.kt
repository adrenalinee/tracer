package malibu.tracer

interface TraceIdGenerator<C> {
    fun generate(logContext: C): String
}