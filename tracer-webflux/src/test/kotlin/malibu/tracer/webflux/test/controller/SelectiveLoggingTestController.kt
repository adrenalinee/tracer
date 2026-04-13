package malibu.tracer.webflux.test.controller

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class SelectiveLoggingTestController {

    @PostMapping("/request-only", consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun requestOnly(@RequestBody body: String): Mono<String> {
        return Mono.just("response:$body")
    }

    @PostMapping("/response-only", consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun responseOnly(@RequestBody body: String): Mono<String> {
        return Mono.just("response:$body")
    }

    @PostMapping("/skip-both", consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun skipBoth(@RequestBody body: String): Mono<String> {
        return Mono.just("response:$body")
    }
}
