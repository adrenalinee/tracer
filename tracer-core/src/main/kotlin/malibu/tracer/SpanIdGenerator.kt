package malibu.tracer

interface SpanIdGenerator<C> {
    fun generate(logContext: C): Int
}