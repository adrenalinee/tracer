package malibu.tracer.webmvc

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders

fun <T> HttpServletRequest.getAttributeOrNull(attributeName: String): T? {
    val attIter =  this.attributeNames.iterator()
    while (attIter.hasNext()) {
        val name = attIter.next()
        if (name != attributeName) {
            continue
        }

        return this.getAttribute(name) as T
    }

    return null
}

fun HttpServletRequest.getHeaderValueOrNull(headerName: String): String? {
    val headerEnum = this.headerNames
    while (headerEnum.hasMoreElements()) {
        val name = headerEnum.nextElement()
        if (name != headerName) {
            continue
        }

        return this.getHeader(name)
    }

    return null
}

fun HttpServletRequest.getPath(): String {
    return this.queryString?.let {
        "${this.requestURI}?${this.queryString}"
    } ?: this.requestURI.toString()
}

fun HttpServletRequest.getUrl(): String {
    return this.queryString?.let {
        "${this.requestURL}?${this.queryString}"
    } ?: this.requestURL.toString()
}

fun HttpServletRequest.getHttpHeaders(): HttpHeaders {
    return this.headerNames.toList()
        .map { it to this.getHeaders(it).toList() }
        .fold(HttpHeaders()) { headers, pair ->
            headers[pair.first] = pair.second

            headers
        }
}