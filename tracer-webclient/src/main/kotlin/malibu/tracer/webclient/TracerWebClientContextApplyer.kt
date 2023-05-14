package malibu.tracer.webclient

class TracerWebClientContextApplyer(
    var traceRequestBody: Boolean? = null,
    var traceResponseBody: Boolean? = null,
    var traceRequestHeaders: Boolean? = null,
    var traceResponseHeaders: Boolean? = null,

//    /**
//     * 상세정보를 추가로 출력할지 결정
//     */
//    var detailLogging: Boolean? = null
) {

    fun apply(webClientContext: TracerWebClientContext) {
        traceRequestBody?.also { webClientContext.traceRequestBody = it }
        traceResponseBody?.also { webClientContext.traceResponseBody = it }
        traceRequestHeaders?.also { webClientContext.traceRequestHeaders = it }
        traceResponseHeaders?.also { webClientContext.traceResponseHeaders = it }

//        detailLogging?.also { webClientContext.detailLogging = it }
    }
}