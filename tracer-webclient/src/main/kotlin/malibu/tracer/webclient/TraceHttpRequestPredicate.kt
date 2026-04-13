package malibu.tracer.webclient

import org.springframework.web.reactive.function.client.ClientRequest

fun interface TraceHttpRequestPredicate {

    fun test(request: ClientRequest): Boolean

    infix fun and(other: TraceHttpRequestPredicate): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { request ->
            this.test(request) && other.test(request)
        }
    }
}
