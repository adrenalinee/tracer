package malibu.tracer.webmvc

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

class ServletExchange(
    val request: HttpServletRequest,
    val response: HttpServletResponse
) {
    private val logger = KotlinLogging.logger {}

    val startedTime = System.currentTimeMillis()

    fun genRequestBody(): String? {
        if (request.contentType != MediaType.APPLICATION_JSON_VALUE &&
            request.contentType.lowercase().startsWith("text").not()) {
            logger.debug {
                "body 출력을 허용하지 않는 contentType 입니다. " +
                        "contentType: ${request.contentType}, contentLength: ${request.contentLength}"
            }
            return null
        }

        if (request.contentLength <= 0) {
            logger.debug { "request body length가 0 입니다." }
            return null
        }

        return if (request is ContentCachingRequestWrapper) {
            String(request.contentAsByteArray)
        } else {
            null
        }
    }

    fun genResponseBody(): String? {
        return if (response is ContentCachingResponseWrapper) {
            if (response.contentSize > 0) {
                String(response.contentAsByteArray)
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