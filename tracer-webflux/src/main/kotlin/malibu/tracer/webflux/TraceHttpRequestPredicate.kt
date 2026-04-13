package malibu.tracer.webflux

import org.springframework.http.server.reactive.ServerHttpRequest

fun interface TraceHttpRequestPredicate {

    fun test(request: ServerHttpRequest): Boolean

    infix fun and(other: TraceHttpRequestPredicate): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { request ->
            this.test(request) && other.test(request)
        }
    }
}
