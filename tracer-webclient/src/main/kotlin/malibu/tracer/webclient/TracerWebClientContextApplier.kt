package malibu.tracer.webclient

import org.springframework.web.reactive.function.client.ClientRequest

class TracerWebClientContextApplier(
    var traceRequestBody: Boolean? = null,
    var traceResponseBody: Boolean? = null,
    var traceRequestHeaders: Boolean? = null,
    var traceResponseHeaders: Boolean? = null,

//    /**
//     * 상세정보를 추가로 출력할지 결정
//     */
//    var detailLogging: Boolean? = null
) {

    private val excludePathPatterns = mutableListOf<String>()
    private val requestTracePredicates = mutableListOf<TraceHttpRequestPredicate>()
    private val responseTracePredicates = mutableListOf<TraceHttpRequestPredicate>()

    /**
     * @param pathPattern - ant path pattern
     */
    fun addExcludePathPatterns(pathPattern: String): TracerWebClientContextApplier {
        excludePathPatterns.add(pathPattern)
        addTracePredicate(TracerWebClientLoggingPredicates.excludePathPattern(pathPattern))
        return this
    }

    fun addTracePredicate(predicate: TraceHttpRequestPredicate): TracerWebClientContextApplier {
        addRequestTracePredicate(predicate)
        addResponseTracePredicate(predicate)
        return this
    }

    fun addTracePredicate(predicate: (ClientRequest) -> Boolean): TracerWebClientContextApplier {
        return addTracePredicate(TraceHttpRequestPredicate(predicate))
    }

    fun addRequestTracePredicate(predicate: TraceHttpRequestPredicate): TracerWebClientContextApplier {
        requestTracePredicates.add(predicate)
        return this
    }

    fun addRequestTracePredicate(predicate: (ClientRequest) -> Boolean): TracerWebClientContextApplier {
        return addRequestTracePredicate(TraceHttpRequestPredicate(predicate))
    }

    fun addResponseTracePredicate(predicate: TraceHttpRequestPredicate): TracerWebClientContextApplier {
        responseTracePredicates.add(predicate)
        return this
    }

    fun addResponseTracePredicate(predicate: (ClientRequest) -> Boolean): TracerWebClientContextApplier {
        return addResponseTracePredicate(TraceHttpRequestPredicate(predicate))
    }

    fun apply(webClientContext: TracerWebClientContext) {
        traceRequestBody?.also { webClientContext.traceRequestBody = it }
        traceResponseBody?.also { webClientContext.traceResponseBody = it }
        traceRequestHeaders?.also { webClientContext.traceRequestHeaders = it }
        traceResponseHeaders?.also { webClientContext.traceResponseHeaders = it }

//        detailLogging?.also { webClientContext.detailLogging = it }

        excludePathPatterns.forEach { webClientContext.excludePathPatterns.add(it) }
        webClientContext.requestTracePredicates.addAll(requestTracePredicates)
        webClientContext.responseTracePredicates.addAll(responseTracePredicates)
    }
}
