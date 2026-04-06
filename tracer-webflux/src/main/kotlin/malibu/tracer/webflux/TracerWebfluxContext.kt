package malibu.tracer.webflux

import org.springframework.http.server.reactive.ServerHttpRequest

class TracerWebfluxContext {

    var traceRequestBody: Boolean = false

    var traceResponseBody: Boolean = false

    var traceRequestHeaders: Boolean = false

    var traceResponseHeaders: Boolean = false


//    /**
//     * 상세정보를 추가로 출력할지 결정
//     */
//    var detailLogging: Boolean = false

    /**
     * 해더를 하나의 필드로 출력할지, 각 해더를 하나의 필드로 출력할지 결정.
     */
    var mergedHeader: Boolean = false


    /**
     * 최대 바디 로깅 길이 (byte 기준)
     */
    var maxPayloadLength: Int = 64 * 1024


    /**
     * 에러가 발생했을 경우에 에러 trace 로그를 출력할지 결정.
     */
    var tracedError: Boolean = true

    /**
     * trace log를 출력하지 않을 request path
     * ant path 스타일
     */
    var excludePathPaterns = mutableListOf<String>()

    internal val requestTracePredicates = mutableListOf<TraceHttpRequestPredicate>()

    internal val responseTracePredicates = mutableListOf<TraceHttpRequestPredicate>()

    fun shouldTraceRequest(request: ServerHttpRequest): Boolean {
        return requestTracePredicates.all { it.test(request) }
    }

    fun shouldTraceResponse(request: ServerHttpRequest): Boolean {
        return responseTracePredicates.all { it.test(request) }
    }
}
