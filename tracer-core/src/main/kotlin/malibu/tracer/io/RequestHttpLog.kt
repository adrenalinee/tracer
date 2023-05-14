package malibu.tracer.io

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.springframework.util.MultiValueMap

/**
 * http request 에 대한 정보
 */
@JsonPropertyOrder("type", "protocol", "method", "url", "path", "headers", "body", "data")
data class RequestHttpLog(

    /**
     * http method, GET, POST..
     */
    val method: String? = null,

    /**
     * 요청 들어온 url
     */
    val url: String? = null,

    /**
     * http, https ..
     */
//    val scheme: String? = null,

    /**
     * 도메인 주소
     */
//    val host: String? = null,

    /**
     * url에서 host 다음부터 ? 이전까지의 문자
     */
    val path: String? = null,

    /**
     * url에서 ? 이후의 문자
     */
//    val queryString: String? = null,

    /**
     * string으로 변환된 응답 해더들 정보
     */
    val headers: MultiValueMap<String, String>? = null,

    /**
     * request body
     */
    val body: String? = null
): TraceLog(
    type = DefinedTraceLogType.REQUEST,
    protocol = DefinedProtocolType.HTTP
)
