package malibu.tracer.webmvc

class TracerWebMvcContextApplyer(
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

    private val excludePathPaterns = mutableListOf<String>()

    /**
     * @param pathPattern - ant path pattern
     */
    fun addExcludePathPaterns(pathPattern: String): TracerWebMvcContextApplyer {
        excludePathPaterns.add(pathPattern)
        return this
    }

    internal fun apply(tracerWebMvcContext: TracerWebMvcContext) {
        traceRequestBody?.also { tracerWebMvcContext.traceRequestBody = it }
        traceResponseBody?.also { tracerWebMvcContext.traceResponseBody = it }
        traceRequestHeaders?.also { tracerWebMvcContext.traceRequestHeaders = it }
        traceResponseHeaders?.also { tracerWebMvcContext.traceResponseHeaders = it }

//        detailLogging?.also { tracerWebMvcContext.detailLogging = it }

        mergedHeader?.also { tracerWebMvcContext.mergedHeader = it }
        tracedError?.also { tracerWebMvcContext.tracedError = it }

        excludePathPaterns.forEach { tracerWebMvcContext.excludePathPaterns.add(it) }
    }
}