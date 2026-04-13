package malibu.tracer.webclient.test.controller

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SelectiveLoggingTestController {

    @PostMapping(
        "/request-only",
        "/response-only",
        "/skip-both",
        "/header-controlled",
        consumes = [MediaType.TEXT_PLAIN_VALUE],
        produces = [MediaType.TEXT_PLAIN_VALUE]
    )
    fun echo(@RequestBody body: String): String {
        return "response:$body"
    }
}
