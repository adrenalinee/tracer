package malibu.tracer.webmvc

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders

fun HttpServletResponse.getHttpHeaders(): HttpHeaders {
    return this.headerNames.toList()
        .map { it to this.getHeaders(it).toList() }
        .fold(HttpHeaders()) { headers, pair ->
            headers[pair.first] = pair.second

            headers
        }
}