package malibu.tracer

class SimpleSpanIdGenerator: malibu.tracer.SpanIdGenerator<Any> {
    override fun generate(logContext: Any): Int {
        return 1
    }
}