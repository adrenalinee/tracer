package malibu.tracer.io

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.springframework.util.MultiValueMap

/**
 * http call(request, response) 대한 정보
 */
@JsonPropertyOrder("type", "protocol", "elapsedTime", "status", "url", "method", "reqHeaders", "resHeaders", "data")
data class ConnectHttpLog(

    /**
     * 요청하는 url
     */
    val url: String? = null,

    /**
     * http, https ..
     */
    val scheme: String? = null,

    /**
     * 도메인 주소
     */
    val host: String? = null,

    /**
     * url에서 host 다음부터 ? 이전까지의 문자
     */
    val path: String? = null,

    /**
     * url에서 ? 이후의 문자
     */
    val queryString: String? = null,

    /**
     * 요청하는 http method, GET, POST..
     */
    val method: String? = null,

    /**
     * string으로 변환된 request headers 정보
     */
    val reqHeaders: MultiValueMap<String, String>? = null,

    /**
     * 요청하는 body
     */
    val reqBody: String? = null,

    /**
     * 요청후 응답받은 status
     * ex: 200, 400, 404 ..
     */
    val status: Int? = null,

    /**
     * string으로 변환된 요청후 응답받은 headers
     */
    val resHeaders: MultiValueMap<String, String>? = null,

    /**
     * 요청후 전달 받은 응답의 body
     */
    val resBody: String? = null,

    /**
     * 요청후 응답받는데까지 걸린 시간
     * unit ms
     */
    val elapsedTime: Long? = null,

    /**
     * http cal 중에 발생한 error 의 trace message
     */
    val error: String? = null
): TraceLog(
    type = DefinedTraceLogType.CONNECT,
    protocol = DefinedProtocolType.HTTP
)
