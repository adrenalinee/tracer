package malibu.tracer.webmvc

import jakarta.servlet.http.HttpServletRequest

class TracerWebMvcContextApplier(
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
    fun addExcludePathPatterns(pathPattern: String): TracerWebMvcContextApplier {
        excludePathPatterns.add(pathPattern)
        addTracePredicate(TracerWebMvcLoggingPredicates.excludePathPattern(pathPattern))
        return this
    }

    @Deprecated("Use addExcludePathPatterns(pathPattern) instead.")
    fun addExcludePathPaterns(pathPattern: String): TracerWebMvcContextApplier {
        return addExcludePathPatterns(pathPattern)
    }

    fun addTracePredicate(predicate: TraceHttpRequestPredicate): TracerWebMvcContextApplier {
        addRequestTracePredicate(predicate)
        addResponseTracePredicate(predicate)
        return this
    }

    fun addTracePredicate(predicate: (HttpServletRequest) -> Boolean): TracerWebMvcContextApplier {
        return addTracePredicate(TraceHttpRequestPredicate(predicate))
    }

    fun addRequestTracePredicate(predicate: TraceHttpRequestPredicate): TracerWebMvcContextApplier {
        requestTracePredicates.add(predicate)
        return this
    }

    fun addRequestTracePredicate(predicate: (HttpServletRequest) -> Boolean): TracerWebMvcContextApplier {
        return addRequestTracePredicate(TraceHttpRequestPredicate(predicate))
    }

    fun addResponseTracePredicate(predicate: TraceHttpRequestPredicate): TracerWebMvcContextApplier {
        responseTracePredicates.add(predicate)
        return this
    }

    fun addResponseTracePredicate(predicate: (HttpServletRequest) -> Boolean): TracerWebMvcContextApplier {
        return addResponseTracePredicate(TraceHttpRequestPredicate(predicate))
    }

    internal fun apply(tracerWebMvcContext: TracerWebMvcContext) {
        traceRequestBody?.also { tracerWebMvcContext.traceRequestBody = it }
        traceResponseBody?.also { tracerWebMvcContext.traceResponseBody = it }
        traceRequestHeaders?.also { tracerWebMvcContext.traceRequestHeaders = it }
        traceResponseHeaders?.also { tracerWebMvcContext.traceResponseHeaders = it }

//        detailLogging?.also { tracerWebMvcContext.detailLogging = it }

        mergedHeader?.also { tracerWebMvcContext.mergedHeader = it }
        tracedError?.also { tracerWebMvcContext.tracedError = it }

        excludePathPatterns.forEach { tracerWebMvcContext.excludePathPaterns.add(it) }
        tracerWebMvcContext.requestTracePredicates.addAll(requestTracePredicates)
        tracerWebMvcContext.responseTracePredicates.addAll(responseTracePredicates)
    }
}
