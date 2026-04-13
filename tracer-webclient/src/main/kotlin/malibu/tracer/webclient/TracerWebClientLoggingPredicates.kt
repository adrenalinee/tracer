package malibu.tracer.webclient

import org.springframework.util.AntPathMatcher
import org.springframework.web.reactive.function.client.ClientRequest

object TracerWebClientLoggingPredicates {

    private val antPathMatcher = AntPathMatcher()

    fun includeAll(): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { true }
    }

    fun excludePathPattern(pathPattern: String): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { request ->
            antPathMatcher.match(pathPattern, request.url().path).not()
        }
    }

    fun excludePathPatterns(vararg pathPatterns: String): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { request ->
            pathPatterns.none { pathPattern ->
                antPathMatcher.match(pathPattern, request.url().path)
            }
        }
    }

    fun includeWhen(predicate: (ClientRequest) -> Boolean): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate(predicate)
    }
}
