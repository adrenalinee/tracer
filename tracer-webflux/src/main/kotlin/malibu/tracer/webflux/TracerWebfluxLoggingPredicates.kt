package malibu.tracer.webflux

import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.AntPathMatcher

object TracerWebfluxLoggingPredicates {

    private val antPathMatcher = AntPathMatcher()

    fun includeAll(): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { true }
    }

    fun excludePathPattern(pathPattern: String): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { request ->
            antPathMatcher.match(pathPattern, request.uri.path).not()
        }
    }

    fun excludePathPatterns(vararg pathPatterns: String): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { request ->
            pathPatterns.none { pathPattern ->
                antPathMatcher.match(pathPattern, request.uri.path)
            }
        }
    }

    fun includeWhen(predicate: (ServerHttpRequest) -> Boolean): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate(predicate)
    }
}
