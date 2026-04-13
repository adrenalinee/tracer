package malibu.tracer.webclient

import org.springframework.web.reactive.function.client.ClientRequest

class TracerWebClientContext {
    var traceRequestBody: Boolean = false

    var traceResponseBody: Boolean = false

    var traceRequestHeaders: Boolean = false

    var traceResponseHeaders: Boolean = false

//    /**
//     * 상세정보를 추가로 출력할지 결정
//     */
//    var detailLogging: Boolean = false

    /**
     * 최대 바디 로깅 길이 (byte 기준)
     */
    var maxPayloadLength: Int = 64 * 1024

    /**
     * trace log를 출력하지 않을 request path
     * ant path 스타일
     */
    var excludePathPatterns = mutableListOf<String>()

    internal val requestTracePredicates = mutableListOf<TraceHttpRequestPredicate>()

    internal val responseTracePredicates = mutableListOf<TraceHttpRequestPredicate>()

    fun shouldTraceRequest(request: ClientRequest): Boolean {
        return requestTracePredicates.all { it.test(request) }
    }

    fun shouldTraceResponse(request: ClientRequest): Boolean {
        return responseTracePredicates.all { it.test(request) }
    }
}
