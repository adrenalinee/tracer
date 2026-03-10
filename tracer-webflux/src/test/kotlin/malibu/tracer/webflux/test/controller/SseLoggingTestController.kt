package malibu.tracer.webflux.test.controller

import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class SseLoggingTestController {

    @GetMapping("/sse", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getSse(): Flux<ServerSentEvent<String>> {
        return Flux.just(
            ServerSentEvent.builder("first").id("1").event("message").build(),
            ServerSentEvent.builder("second").id("2").event("message").build()
        )
    }
}
