package malibu.tracer.webclient

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
}