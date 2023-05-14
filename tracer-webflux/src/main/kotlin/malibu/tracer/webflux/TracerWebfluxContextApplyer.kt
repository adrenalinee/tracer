package malibu.tracer.webflux

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

    private val excludePathPaterns = mutableListOf<String>()

    /**
     * @param pathPattern - ant path pattern
     */
    fun addExcludePathPaterns(pathPattern: String): TracerWebfluxContextApplyer {
        excludePathPaterns.add(pathPattern)
        return this
    }

    internal fun apply(tracerWebfluxContext: TracerWebfluxContext) {
        traceRequestBody?.also { tracerWebfluxContext.traceRequestBody = it }
        traceResponseBody?.also { tracerWebfluxContext.traceResponseBody = it }
        traceRequestHeaders?.also { tracerWebfluxContext.traceRequestHeaders = it }
        traceResponseHeaders?.also { tracerWebfluxContext.traceResponseHeaders = it }

//        detailLogging?.also { tracerWebfluxContext.detailLogging = it }

        mergedHeader?.also { tracerWebfluxContext.mergedHeader = it }
        tracedError?.also { tracerWebfluxContext.tracedError = it }

        excludePathPaterns.forEach { tracerWebfluxContext.excludePathPaterns.add(it) }
    }
}