package malibu.tracer.webflux

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.http.HttpHeaders

fun HttpHeaders.toMultiValueMap(): MultiValueMap<String, String> {
    return LinkedMultiValueMap<String, String>().also { map ->
        forEach { name, values ->
            map.put(name, values)
        }
    }
}