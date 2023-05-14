package malibu.tracer

import java.util.*

class SimpleTraceIdGenerator: malibu.tracer.TraceIdGenerator<Any> {
    override fun generate(logContext: Any): String {
        return UUID.randomUUID().toString()
    }
}