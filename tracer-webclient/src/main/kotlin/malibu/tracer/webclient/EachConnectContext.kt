package malibu.tracer.webclient

import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse

class EachConnectContext(
    val request: ClientRequest,
    val response: ClientResponse? = null,
    val throwable: Throwable? = null
)