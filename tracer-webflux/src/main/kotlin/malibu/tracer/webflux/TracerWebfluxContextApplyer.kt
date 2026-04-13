package malibu.tracer.webflux

import org.springframework.http.server.reactive.ServerHttpRequest

class TracerWebfluxContextApplyer(
    var traceRequestBody: Boolean? = null,
    var traceResponseBody: Boolean? = null,
    var traceRequestHeaders: Boolean? = null,
    var traceResponseHeaders: Boolean? = null,

//    /**
//     * 상세정보를 추가로 출력할지 결정
//     */
//    var detailLogging: Boolean? = null,

    /**
     * 해더를 하나의 필드로 출력할지, 각 해더를 하나의 필드로 출력할지 결정.
     * 주의: 아직 동작하지 않음!!
     */
    var mergedHeader: Boolean? = null,

    /**
     * 에러가 발생했을 경우에 에러 trace 로그를 출력할지 결정.
     */
    var tracedError: Boolean? = null
) {

    private val excludePathPatterns = mutableListOf<String>()
    private val requestTracePredicates = mutableListOf<TraceHttpRequestPredicate>()
    private val responseTracePredicates = mutableListOf<TraceHttpRequestPredicate>()

    /**
     * @param pathPattern - ant path pattern
     */
    fun addExcludePathPatterns(pathPattern: String): TracerWebfluxContextApplyer {
        excludePathPatterns.add(pathPattern)
        addTracePredicate(TracerWebfluxLoggingPredicates.excludePathPattern(pathPattern))
        return this
    }

    fun addTracePredicate(predicate: TraceHttpRequestPredicate): TracerWebfluxContextApplyer {
        addRequestTracePredicate(predicate)
        addResponseTracePredicate(predicate)
        return this
    }

    fun addTracePredicate(predicate: (ServerHttpRequest) -> Boolean): TracerWebfluxContextApplyer {
        return addTracePredicate(TraceHttpRequestPredicate(predicate))
    }

    fun addRequestTracePredicate(predicate: TraceHttpRequestPredicate): TracerWebfluxContextApplyer {
        requestTracePredicates.add(predicate)
        return this
    }

    fun addRequestTracePredicate(predicate: (ServerHttpRequest) -> Boolean): TracerWebfluxContextApplyer {
        return addRequestTracePredicate(TraceHttpRequestPredicate(predicate))
    }

    fun addResponseTracePredicate(predicate: TraceHttpRequestPredicate): TracerWebfluxContextApplyer {
        responseTracePredicates.add(predicate)
        return this
    }

    fun addResponseTracePredicate(predicate: (ServerHttpRequest) -> Boolean): TracerWebfluxContextApplyer {
        return addResponseTracePredicate(TraceHttpRequestPredicate(predicate))
    }

    internal fun apply(tracerWebfluxContext: TracerWebfluxContext) {
        traceRequestBody?.also { tracerWebfluxContext.traceRequestBody = it }
        traceResponseBody?.also { tracerWebfluxContext.traceResponseBody = it }
        traceRequestHeaders?.also { tracerWebfluxContext.traceRequestHeaders = it }
        traceResponseHeaders?.also { tracerWebfluxContext.traceResponseHeaders = it }

//        detailLogging?.also { tracerWebfluxContext.detailLogging = it }

        mergedHeader?.also { tracerWebfluxContext.mergedHeader = it }
        tracedError?.also { tracerWebfluxContext.tracedError = it }

        excludePathPatterns.forEach { tracerWebfluxContext.excludePathPaterns.add(it) }
        tracerWebfluxContext.requestTracePredicates.addAll(requestTracePredicates)
        tracerWebfluxContext.responseTracePredicates.addAll(responseTracePredicates)
    }
}
