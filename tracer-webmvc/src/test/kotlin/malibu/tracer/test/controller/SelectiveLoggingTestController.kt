package malibu.tracer.test.controller

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SelectiveLoggingTestController {

    @PostMapping("/request-only", consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun requestOnly(@RequestBody body: String): String {
        return "response:$body"
    }

    @PostMapping("/response-only", consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun responseOnly(@RequestBody body: String): String {
        return "response:$body"
    }

    @PostMapping("/skip-both", consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
    fun skipBoth(@RequestBody body: String): String {
        return "response:$body"
    }
}
