package malibu.tracer.webmvc

import jakarta.servlet.http.HttpServletRequest
import org.springframework.util.AntPathMatcher

object TracerWebMvcLoggingPredicates {

    private val antPathMatcher = AntPathMatcher()

    fun includeAll(): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { true }
    }

    fun excludePathPattern(pathPattern: String): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { request ->
            antPathMatcher.match(pathPattern, request.getTraceRequestUriPath()).not()
        }
    }

    fun excludePathPatterns(vararg pathPatterns: String): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate { request ->
            pathPatterns.none { pathPattern ->
                antPathMatcher.match(pathPattern, request.getTraceRequestUriPath())
            }
        }
    }

    fun includeWhen(predicate: (HttpServletRequest) -> Boolean): TraceHttpRequestPredicate {
        return TraceHttpRequestPredicate(predicate)
    }
}
