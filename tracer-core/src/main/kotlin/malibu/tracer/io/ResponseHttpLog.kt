package malibu.tracer.io

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.springframework.util.MultiValueMap

/**
 * http response에 대한 정보
 */
@JsonPropertyOrder("type", "protocol", "elapsedTime", "method", "status", "url", "path", "headers", "body", "error", "data")
data class ResponseHttpLog(

    /**
     * http method, GET, POST..
     */
    val method: String? = null,

    /**
     * 응답 status
     * ex: 200, 400, 404 ..
     */
    val status: Int? = null,


    /**
     * 요청 들어온 url
     */
    val url: String? = null,

    /**
     * url에서 host 다음부터 ? 이전까지의 문자
     */
    val path: String? = null,

    /**
     * string으로 변환된 응답 해더들 정보
     */
    val headers: MultiValueMap<String, String>? = null,

    /**
     * response body
     */
    val body: String? = null,

    /**
     * request 를 받은 이후에 응답할때까지 걸린 시간
     * * unit ms
     */
    val elapsedTime: Long? = null,

    /**
     * request 처리중에 핸들링 되지 않고 최종 에러처리(controller 밖으로 throw.., 500 에러 처리) 됬을 경우에 trace message를 이필드에 담음.
     * 정상동작했을때는 비어 있음.
     */
    val error: String? = null
): TraceLog(
    type = DefinedTraceLogType.RESPONSE,
    protocol = DefinedProtocolType.HTTP
)