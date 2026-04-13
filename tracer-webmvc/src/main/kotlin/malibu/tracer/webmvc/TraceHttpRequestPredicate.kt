package malibu.tracer.webmvc

import jakarta.servlet.http.HttpServletRequest

fun interface TraceHttpRequestPredicate {

    fun test(request: HttpServletRequest): Boolean

    infix fun and(other: TraceHttpRequestPredicate): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { request ->
            this.test(request) && other.test(request)
        }
    }
}
