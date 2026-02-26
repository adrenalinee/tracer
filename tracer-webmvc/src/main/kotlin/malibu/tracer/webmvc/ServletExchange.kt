package malibu.tracer.webmvc

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import malibu.tracer.io.toLimitedString
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

class ServletExchange(
    val request: HttpServletRequest,
    val response: HttpServletResponse,
    private val maxPayloadLength: Int
) {
    private val logger = KotlinLogging.logger {}

    val startedTime = System.currentTimeMillis()

    fun genRequestBody(): String? {
        if (request.contentType.isNullOrEmpty()) {
            logger.debug {
                "contentType 이 정의되지 않아서 request body 를 전달하지 않습니다. (어떤 데이터인지 알 수 없어서 읽지 않습니다.)" +
                        "contentType: ${request.contentType}, contentLength: ${request.contentLength}"
            }
            return null
        }
        if (request.contentType != MediaType.APPLICATION_JSON_VALUE &&
            request.contentType.lowercase().startsWith("text").not()
        ) {
            logger.debug {
                "body 읽기를 허용하지 않는 contentType 입니다. (어떤 데이터인지 알 수 없어서 읽지 않습니다.) contentType: ${request.contentType}" +
                        "contentType: ${request.contentType}, contentLength: ${request.contentLength}"
            }
            return null
        }

        if (request.contentLength <= 0) {
            logger.debug { "request body length가 0 입니다." }
            return null
        }

        return if (request is ContentCachingRequestWrapper) {
            val content = request.contentAsByteArray
            if (content.isEmpty()) {
                null
            } else {
                val truncated = maxPayloadLength > 0 && content.size >= maxPayloadLength
                content.toLimitedString(maxPayloadLength, truncated = truncated)
            }
        } else {
            null
        }
    }

    fun genResponseBody(): String? {
        return if (response is ContentCachingResponseWrapper) {
            if (response.contentSize > 0) {
                val content = response.contentAsByteArray
                val truncated = maxPayloadLength > 0 && content.size >= maxPayloadLength
                content.toLimitedString(maxPayloadLength, truncated = truncated)
            } else {
                null
            }
        } else {
            null
        }
    }

    /**
     * request body 의 길이
     */
    fun getRequestBodySize(): Int {
        return request.contentLength
    }

    /**
     * response body 의 길이
     */
    fun getResponseBodySize(): Int {
        return if (response is ContentCachingResponseWrapper) {
            response.contentSize
        } else {
             -1 //알 수 없음.
        }
    }
}